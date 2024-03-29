/*
 * Copyright 2013 Mikhail Titov.
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
package org.onesec.raven.ivr.actions;

import org.onesec.raven.ivr.IvrEndpointConversation;

/**
 *
 * @author Mikhail Titov
 */
public class SimpleTransferCallAction extends AsyncAction {
    public final static String NAME = "Simple transfer";
    private final String address;

    public SimpleTransferCallAction(String address) {
        super(NAME);
        this.address = address;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception {
        if (logger.isDebugEnabled())
            logger.debug("Transfering call to address ({})", address);
        conversation.transfer(address);
    }

    public boolean isFlowControlAction() {
        return false;
    }
}
