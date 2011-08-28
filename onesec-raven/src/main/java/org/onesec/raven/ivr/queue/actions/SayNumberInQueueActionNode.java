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

package org.onesec.raven.ivr.queue.actions;

import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class SayNumberInQueueActionNode extends BaseNode implements IvrActionNode
{
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private Node numbersNode;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private AudioFileNode preambleAudio;

    @NotNull @Parameter(defaultValue="10")
    private Long pauseBeetweenWords;

    public IvrAction createAction() {
        return new SayNumberInQueueAction(numbersNode, pauseBeetweenWords, preambleAudio);
    }

    public Node getNumbersNode() {
        return numbersNode;
    }

    public void setNumbersNode(Node numbersNode) {
        this.numbersNode = numbersNode;
    }

    public Long getPauseBeetweenWords() {
        return pauseBeetweenWords;
    }

    public void setPauseBeetweenWords(Long pauseBeetweenWords) {
        this.pauseBeetweenWords = pauseBeetweenWords;
    }

    public AudioFileNode getPreambleAudio() {
        return preambleAudio;
    }

    public void setPreambleAudio(AudioFileNode preambleAudio) {
        this.preambleAudio = preambleAudio;
    }
}