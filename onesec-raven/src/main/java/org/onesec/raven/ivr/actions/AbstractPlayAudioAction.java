/*
 *  Copyright 2011 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.actions;

import java.io.InputStream;
import javax.script.Bindings;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.IvrUtils;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractPlayAudioAction extends AsyncAction implements InputStreamSource
{
    private AudioFile audioFile;
    private Bindings bindings;

    public AbstractPlayAudioAction(String actionName)
    {
        super(actionName);
    }

    protected abstract AudioFile getAudioFile(IvrEndpointConversation conversation);
    
    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        audioFile = getAudioFile(conversation);
        bindings = conversation.getConversationScenarioState().getBindings();
        if (audioFile==null){
            if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
                conversation.getOwner().getLogger().debug(logMess("Nothing to play"));
            return;
        }
        if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
            conversation.getOwner().getLogger().debug(
                    logMess("Playing audio from source (%s)", audioFile.getPath()));
        IvrUtils.playAudioInAction(this, conversation, this, audioFile);
        if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
            conversation.getOwner().getLogger().debug(logMess(
                    "Audio source (%s) successfuly played ", audioFile.getPath()));
    }

    public boolean isFlowControlAction() {
        return false;
    }

    public InputStream getInputStream()
    {
        try {
            BindingSupport bindingSupport = audioFile.getBindingSupport();
            try {
                if (bindingSupport!=null)
                    bindingSupport.putAll(bindings);
                return audioFile.getAudioFile().getDataStream();
            } finally {
                if (bindingSupport!=null)
                    bindingSupport.reset();
            }
        }
        catch (Exception ex) {
            if (conversation.getOwner().isLogLevelEnabled(LogLevel.ERROR))
                conversation.getOwner().getLogger().error(
                    logMess("Error geting audio stream from audio file node (%s) ", audioFile.getPath())
                    , ex);
            return null;
        }
    }
}
