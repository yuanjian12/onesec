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

import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;

/**
 *
 * @author Mikhail Titov
 */
public class AuthenticateOperatorAction extends AsyncAction {
    
    public final static String ACTION_NAME = "Authenticate operator";

    public AuthenticateOperatorAction(String actionName) {
        super(actionName);
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isFlowControlAction() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}