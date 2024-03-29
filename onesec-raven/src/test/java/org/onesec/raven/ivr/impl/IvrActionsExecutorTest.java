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

package org.onesec.raven.ivr.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.script.Bindings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionStatus;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.DtmfProcessPointAction;
import org.onesec.raven.ivr.actions.PauseAction;
import org.raven.conv.ConversationScenarioState;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.ContainerNode;
import static org.easymock.EasyMock.*;
//import 
/**
 *
 * @author Mikhail Titov
 */
public class IvrActionsExecutorTest extends RavenCoreTestCase
{
    private ExecutorServiceNode executor;
    private IvrEndpointNode endpointNode;
    private IvrEndpointConversation conversation;

    @Before
    public void prepare()
    {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setMaximumPoolSize(1);
        executor.setCorePoolSize(1);
        assertTrue(executor.start());

        endpointNode = new IvrEndpointNode();
        endpointNode.setName("endpoint");
        tree.getRootNode().addAndSaveChildren(endpointNode);
        
        ContainerNode conversationOwner = new ContainerNode("conversation owner");
        tree.getRootNode().addAndSaveChildren(conversationOwner);
        conversationOwner.setLogLevel(LogLevel.TRACE);
        assertTrue(conversationOwner.start());

        conversation = createMock(IvrEndpointConversation.class);
        expect(conversation.getOwner()).andReturn(conversationOwner).anyTimes();
        replay(conversation);
    }

    @After
    public void testMocks()
    {
        verify(conversation);
    }

//    @Test
    public void executeTest() throws ExecutorServiceException, InterruptedException
    {
        List<IvrAction> actions = Arrays.asList(
                (IvrAction)new TestPauseAction(), new TestPauseAction());
        IvrActionsExecutor actionsExecutor = new IvrActionsExecutor(conversation, executor);
        actionsExecutor.executeActions(actions);
        Thread.sleep(1100);
        assertEquals(IvrActionStatus.EXECUTED, actions.get(0).getStatus());
        assertEquals(IvrActionStatus.EXECUTED, actions.get(1).getStatus());
    }

//    @Test
    public void cancelTest() throws Exception
    {
        List<IvrAction> actions = Arrays.asList(
                (IvrAction)new TestPauseAction(), new TestPauseAction());
        List<IvrAction> newActions = Arrays.asList((IvrAction)new TestPauseAction());
        IvrActionsExecutor actionsExecutor = new IvrActionsExecutor(conversation, executor);
        actionsExecutor.executeActions(actions);
        actionsExecutor.executeActions(newActions);
        Thread.sleep(600);
        assertEquals(IvrActionStatus.EXECUTED, actions.get(0).getStatus());
        assertTrue(((TestPauseAction)actions.get(0)).isCanceled());
        assertEquals(IvrActionStatus.WAITING, actions.get(1).getStatus());
        assertFalse(((TestPauseAction) actions.get(1)).isCanceled());
        
        assertEquals(IvrActionStatus.EXECUTED, newActions.get(0).getStatus());
        assertFalse(((TestPauseAction)newActions.get(0)).isCanceled());
    }

    @Test
    public void dtmfProcessPointTest() throws Exception
    {
        executor.setMaximumPoolSize(10);
        executor.setCorePoolSize(10);
        executor.stop();
        executor.start();
        
        IvrEndpointConversation endpoint = createMock("endpoint", IvrEndpointConversation.class);
        Node owner = createMock("ownerNode", Node.class);
        ConversationScenarioState state = createMock("conversationState", ConversationScenarioState.class);
        Bindings bindings = createMock("bindings", Bindings.class);

        endpoint.continueConversation('-');
        expect(endpoint.getOwner()).andReturn(owner).anyTimes();
        expect(endpoint.getExecutorService()).andReturn(executor).anyTimes();
        expect(owner.isLogLevelEnabled((LogLevel)anyObject())).andReturn(Boolean.FALSE).anyTimes();
        expect(owner.getPath()).andReturn("endpoint").anyTimes();

        expect(endpoint.getConversationScenarioState()).andReturn(state).once();
        expect(state.getBindings()).andReturn(bindings).once();
        expect(bindings.put(eq(IvrEndpointConversation.DTMFS_BINDING), eq(Arrays.asList('2', '1')))).andReturn(null).once();

        replay(endpoint, owner, state, bindings);

        IvrActionsExecutor actionsExecutor = new IvrActionsExecutor(endpoint, executor);

        List<IvrAction> actions = new ArrayList<IvrAction>();
        actions.add(new PauseAction(1000));
        actions.add(new DtmfProcessPointAction("12"));
        actions.add(new PauseAction(1000));
        actionsExecutor.executeActions(actions);
        assertTrue(actionsExecutor.hasDtmfProcessPoint('2'));
        assertTrue(actionsExecutor.hasDtmfProcessPoint('1'));
        assertFalse(actionsExecutor.hasDtmfProcessPoint('-'));
        Thread.sleep(1100);
        assertFalse(actionsExecutor.hasDtmfProcessPoint('1'));
        
        verify(endpoint, owner, state, bindings);
    }
}
