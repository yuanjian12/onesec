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
package org.onesec.raven.ivr.queue.actions;

import javax.script.Bindings;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;
import org.onesec.raven.ivr.queue.CommutationManagerCall;

/**
 *
 * @author Mikhail Titov
 */
public class CancelQueueRequestByOperatorAction extends AsyncAction {
    private static String NAME = "Cancel queue request action";

    public CancelQueueRequestByOperatorAction() {
        super(NAME);
    }

    @Override
    protected void doExecute(IvrEndpointConversation conv) throws Exception {
        Bindings bindings = conv.getConversationScenarioState().getBindings();
        CommutationManagerCall commutationManager = (CommutationManagerCall) 
                bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING);
        if (commutationManager==null)
            throw new Exception("CallsCommutationManager not found in the conversation scenario state");
        commutationManager.cancel();
    }

    public boolean isFlowControlAction() {
        return false;
    }
    
}
