/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.impl;

import com.ibm.media.codec.audio.PCMToPCM;
import com.ibm.media.codec.audio.rc.RCModule;
import com.sun.media.parser.audio.WavParser;
import javax.media.Demultiplexer;
import javax.media.Format;
import javax.media.Multiplexer;
import javax.media.format.AudioFormat;
import javax.media.protocol.FileTypeDescriptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.onesec.raven.ivr.Codec.*;
import org.onesec.raven.ivr.CodecConfig;
import org.onesec.raven.ivr.CodecManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class CodecManagerImplTest extends Assert {
    
    private CodecManager manager;
    private Logger logger = LoggerFactory.getLogger(CodecManager.class);
    
    @Before
    public void prepare() throws Exception {
        manager = new CodecManagerImpl(logger);
    }
    
//    @Test
    public void formatsTest() throws Exception {
        PCMToPCM codec = new PCMToPCM();
//        for (Format format: codec.getSupportedInputFormats()) {
//            logger.debug("  >> SUPPORTED FORMAT: "+format);
            for (Format oFormat: codec.getSupportedOutputFormats(LINEAR.getAudioFormat()))
                logger.debug("     OUTPUT FORMAT: "+oFormat);
//        }
    }
    
    @Test
    public void buildCodecChainTest() throws Exception {
        long startTs = System.currentTimeMillis();
        CodecConfig[] codecs = manager.buildCodecChain(G729.getAudioFormat(), G729.getAudioFormat());
//        for (int i=1; i<10000; ++i)
//            codecs = manager.buildCodecChain(G711_MU_LAW.getAudioFormat(), G729.getAudioFormat());
        logger.debug("Processing time: {}", System.currentTimeMillis()-startTs);
        assertNotNull(codecs);
        for (CodecConfig codec: codecs) {
            logger.debug("CODEC: {}", codec.getCodec());
            logger.debug("   INPUT  FORMAT: {}", codec.getInputFormat());
            logger.debug("   OUTPUT FORMAT: {}", codec.getOutputFormat());
        }
//        assertEquals(2, codecs.length);
    }
    
//    @Test
    public void buildCodecChainTest2() throws Exception {
        AudioFormat f1 = new AudioFormat(
                AudioFormat.LINEAR, 16000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
        AudioFormat f2 = new AudioFormat(
                AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
        CodecConfig[] codecs = manager.buildCodecChain(f1, f2);
        assertNotNull(codecs);
        assertEquals(1, codecs.length);
        assertEquals(RCModule.class, codecs[0].getCodec().getClass());
        assertEquals(f1, codecs[0].getInputFormat());
        assertEquals(f2, codecs[0].getOutputFormat());
        for (CodecConfig codec: codecs) {
            logger.debug("CODEC: {}", codec.getCodec());
            logger.debug("   INPUT  FORMAT: {}", codec.getInputFormat());
            logger.debug("   OUTPUT FORMAT: {}", codec.getOutputFormat());
        }
//        assertEquals(2, codecs.length);
    }
    
//    @Test
    public void buildMultiplexerTest() throws Exception {
        Multiplexer mux = manager.buildMultiplexer(FileTypeDescriptor.WAVE);
        assertNotNull(mux);
    }
//    @Test
//    public void buildCodecChainTest2() throws Exception {
//        long startTs = System.currentTimeMillis();
//        CodecConfig[] codecs = manager.buildCodecChain(G711_MU_LAW.getAudioFormat(), G729.getAudioFormat());
//        for (int i=1; i<10000; ++i)
//            codecs = manager.buildCodecChain(G711_MU_LAW.getAudioFormat(), G729.getAudioFormat());
//        logger.debug("Processing time: {}", System.currentTimeMillis()-startTs);
//        assertNotNull(codecs);
//        for (CodecConfig codec: codecs) {
//            logger.debug("CODEC: {}", codec.getCodec());
//            logger.debug("   INPUT  FORMAT: {}", codec.getInputFormat());
//            logger.debug("   OUTPUT FORMAT: {}", codec.getOutputFormat());
//        }
////        assertEquals(2, codecs.length);
//    }
    
    
//    @Test
    public void buildDemultiplexerTest() {
        Demultiplexer parser = manager.buildDemultiplexer(FileTypeDescriptor.WAVE);
        assertNotNull(parser);
        assertTrue(parser instanceof WavParser);
    }
}
