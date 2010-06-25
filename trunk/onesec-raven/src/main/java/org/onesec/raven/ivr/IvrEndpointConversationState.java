/*
 *  Copyright 2009 Mikhail Titov.
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

package org.onesec.raven.ivr;

import org.onesec.core.State;

/**
 *
 * @author Mikhail Titov
 */
public interface IvrEndpointConversationState extends State<IvrEndpointConversationState, IvrEndpointConversation>
{
    public final static int READY = 1;
    public final static int TALKING = 2;
    public final static int TRANSFERING = 3;
    public final static int INVALID = 4;
}