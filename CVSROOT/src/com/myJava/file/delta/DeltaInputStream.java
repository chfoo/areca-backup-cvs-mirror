package com.myJava.file.delta;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.myJava.file.delta.bucket.Bucket;
import com.myJava.file.delta.bucket.NewBytesBucket;
import com.myJava.file.delta.bucket.ReadPreviousBucket;
import com.myJava.file.delta.tools.IOHelper;
import com.myJava.util.log.Logger;

/**
 * <BR>
 * @author Olivier PETRUCCI
 * <BR>
 *
 */

 /*
 Copyright 2005-2009, Olivier PETRUCCI.

This file is part of Areca.

    Areca is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Areca is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Areca; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class DeltaInputStream 
extends InputStream
implements Constants, LayerHandler {    
    private InputStream in;
    private List layers = new ArrayList();
    private long position = 0;
    private long baseStreamPosition = 0;

    public void setMainInputStream(InputStream in) {
        this.in = in;
    }

    public void addInputStream(InputStream stream, String name) {	
        layers.add(new DeltaLayer(stream, name));
    }

    public void close() throws IOException {
        if (in != null) {
            in.close();
        }

        Iterator iter = layers.iterator();
        while (iter.hasNext()) {
            DeltaLayer layer = (DeltaLayer)iter.next();
            layer.close();
        }
    }

    public int read() throws IOException {
        byte[] b = new byte[1];
        int ret = read(b);
        if (ret == -1) {
            return -1;
        } else {
            return b[0]& 0xff;
        }
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int available() throws IOException {
        throw new UnsupportedOperationException();
    }

    public long skip(long n) throws IOException {
        throw new UnsupportedOperationException();
    }

    public int read(byte[] buffer, int off, int len) throws IOException {
        int read = 0;

        List instructionsToProcess = new ArrayList();
        List tmp = new ArrayList();
        DeltaReadInstruction init = new DeltaReadInstruction(); // Initial Instruction : initiates the process
        init.setReadFrom(position);
        init.setReadTo(position + len - 1);
        init.setWriteOffset(off);
        instructionsToProcess.add(init);

        for (int i=layers.size() - 1; i>=0; i--) {          // Iterate on all layers
            DeltaLayer layer = (DeltaLayer)layers.get(i);
            if (layer.getCurrentBucket() == null) {
                layer.readNextBucket();
            }

            for (int b = 0; b<instructionsToProcess.size(); b++) {
                DeltaReadInstruction instruction = (DeltaReadInstruction)instructionsToProcess.get(b); // Get the next instruction to process
                long from = instruction.getReadFrom();
                long to = instruction.getReadTo();
                int writeOffset = instruction.getWriteOffset();

                if (layer.getCurrentBucket() == null) {
                	return -1;
                }
                
                // Skip all buckets until we find an appropriate one
                while (! (layer.getCurrentBucket().getFrom() <= from && layer.getCurrentBucket().getTo() >= from )) {
                    Bucket bucket = layer.getCurrentBucket();
                    if (bucket.getSignature() == SIG_NEW) {
                        // If it is a "new bytes" bucket, skip the remaining bytes
                        NewBytesBucket current = (NewBytesBucket)bucket;
                        IOHelper.skipFully(layer.getStream(), bucket.getLength() - current.getReadOffset());
                    }

                    layer.readNextBucket();
                }

                // Process the bucket matching the from/to criteria
                while (layer.getCurrentBucket() != null) { // Iterate on all buckets
                    Bucket bucket = layer.getCurrentBucket();
                    long toSkip = from - bucket.getFrom();
                    int toWrite = (int)Math.min(bucket.getLength() - toSkip, to - from + 1);

                    if (bucket.getSignature() == SIG_NEW) {
                        // Read the data from the stream
                        NewBytesBucket current = (NewBytesBucket)bucket;
                        toSkip -= current.getReadOffset();
                        IOHelper.skipFully(layer.getStream(), toSkip);
                        int readBytes = IOHelper.readFully(layer.getStream(), buffer, writeOffset, toWrite);                   // Read the data from the bucket's stream
                        if (readBytes != toWrite) {
                            Logger.defaultLogger().error("Error processing instruction : " + instruction.toString() + ". Bucket is : " + current.toString());
                            throw new DeltaException("Incoherent read length : expected " + toWrite + ", got " + readBytes + " for diff-layer #" + i);
                        }
                        read += toWrite;
                        current.setReadOffset(current.getReadOffset() + toWrite + toSkip);
                    } else {
                        // Read the data from underlying layer
                        ReadPreviousBucket current = (ReadPreviousBucket)bucket;

                        // Build and add a new bucket to the next list of buckets to process
                        DeltaReadInstruction newInstruction = new DeltaReadInstruction();
                        newInstruction.setReadFrom(current.getReadFrom() + toSkip);
                        newInstruction.setReadTo(newInstruction.getReadFrom() + toWrite - 1);
                        newInstruction.setWriteOffset(writeOffset);

                        // Add bucket for the next layer
                        tmp.add(newInstruction);
                    }
                    from += toWrite;
                    writeOffset += toWrite;

                    if (from == to + 1) {
                        break; // We've processed all required bytes
                    } else {
                        layer.readNextBucket(); // Not finished yet ! -> Read the next bucket
                    }
                }
            }

            instructionsToProcess = tmp; // go to the next list of instructions to process
            tmp = new ArrayList();
        }

        // Process the base stream
        for (int b = 0; b<instructionsToProcess.size(); b++) {
            DeltaReadInstruction instruction = (DeltaReadInstruction)instructionsToProcess.get(b);
            long from = instruction.getReadFrom();
            long to = instruction.getReadTo();
            int writeOffset = instruction.getWriteOffset();

            // Skip unnecessary bytes
            long toSkip = from - baseStreamPosition;
            IOHelper.skipFully(in, toSkip);
            baseStreamPosition += toSkip;

            // Read the required bytes
            int toWrite = (int)(to - from + 1);
            int readBytes = IOHelper.readFully(in, buffer, writeOffset, toWrite);
            if (readBytes != toWrite) {
                Logger.defaultLogger().error("Error processing instruction : " + instruction.toString() + ". Current base stream position is : " + baseStreamPosition + ". Current position is : " + position);
                throw new DeltaException("Incoherent read length : expected " + toWrite + ", got " + readBytes + " for base stream.");
            }
            read += toWrite;
            baseStreamPosition += toWrite;
        }

        position += read;

        return (read == 0 && len != 0) ? -1 : read;
    }    

    public synchronized void reset() throws IOException {
        throw new UnsupportedOperationException("Reset is not supported on this implementation");
    }

    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException("Mark is not supported on this implementation");
    }

    public boolean markSupported() {
        return false;
    }
}
