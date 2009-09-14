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

import java.sql.Timestamp;
import java.util.Collection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.raven.ivr.ConversationCompletionCallback;
import org.onesec.raven.ivr.ConversationResult;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointException;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.IvrInformerStatus;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;
import static org.onesec.raven.ivr.impl.IvrInformerRecordSchemaNode.*;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class IvrInformer 
        extends BaseNode implements DataSource, ConversationCompletionCallback, Task, DataConsumer
{
    public final static String NOT_PROCESSED_STATUS = "NOT_PROCESSED";
    public final static String PROCESSING_ERROR_STATUS = "PROCESSING_ERROR";
    public final static String SKIPPED_STATUS = "SKIPPED";
    public final static String NUMBER_BUSY_STATUS = "NUMBER_BUSY";
    public final static String NUMBER_NOT_ANSWERED_STATUS = "NUMBER_NOT_ANSWERED";
    public final static String COMPLETED_BY_INFORMER_STATUS = "COMPLETED_BY_INFORMER";
    public final static String COMPLETED_BY_ABONENT_STATUS = "COMPLETED_BY_ABONENT";

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executorService;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenarioNode conversationScenario;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrEndpoint endpoint;

    private IvrInformerStatus informerStatus;
    private String statusMessage;
    private Record currentRecord;
    private String lastSuccessfullyProcessedAbonId;
    private Lock recordLock;
    private Condition recordProcessed;
    private ConversationResult conversationResult;

    @Override
    protected void initFields()
    {
        super.initFields();
        recordLock = new ReentrantLock();
        recordProcessed = recordLock.newCondition();
        informerStatus = IvrInformerStatus.NOT_READY;
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        informerStatus = IvrInformerStatus.WAITING;
    }

    @Override
    public boolean start() throws NodeError
    {
        boolean result = super.start();
        try
        {
            if (result)
            {
                lastSuccessfullyProcessedAbonId = null;
                executorService.execute(this);
            }
        } catch (ExecutorServiceException ex)
        {
            throw new NodeError("Error starting node", ex);
        }
        return result;
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        informerStatus = IvrInformerStatus.NOT_READY;
    }

    @Override
    public boolean isAutoStart()
    {
        return false;
    }

    public IvrInformerStatus getInformerStatus()
    {
        return informerStatus;
    }

    public void setInformerStatus(IvrInformerStatus informerStatus)
    {
        this.informerStatus = informerStatus;
    }

    public IvrConversationScenarioNode getConversationScenario()
    {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenarioNode conversationScenario)
    {
        this.conversationScenario = conversationScenario;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public IvrEndpoint getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(IvrEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public ExecutorService getExecutorService()
    {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        return false;
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }

    public void conversationCompleted(ConversationResult conversationResult)
    {
        this.conversationResult = conversationResult;
        recordLock.lock();
        try
        {
            recordProcessed.signal();
        }
        finally
        {
            recordLock.unlock();
        }
    }

    public Node getTaskNode()
    {
        return this;
    }

    public String getStatusMessage()
    {
        return statusMessage;
    }

    public void run()
    {
        informerStatus = IvrInformerStatus.PROCESSING;
        try
        {
            statusMessage = "Requesting records from "+dataSource.getPath();
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(statusMessage);
            dataSource.getDataImmediate(this, null);
            sendDataToConsumers(null);
        }
        finally
        {
            informerStatus = IvrInformerStatus.PROCESSED;
        }
    }

    public void setData(DataSource dataSource, Object data)
    {
        if (!(data instanceof Record))
            return;
        Record rec = (Record) data;
        if (!(rec.getSchema() instanceof IvrInformerRecordSchemaNode))
            return;

        currentRecord = rec;
        try
        {
            if (ObjectUtils.equals(
                    lastSuccessfullyProcessedAbonId, currentRecord.getValue(ABONENT_ID_FIELD)))
            {
                skipRecord();
            }
            else
                informAbonent();
            sendDataToConsumers(currentRecord);
        }
        catch(Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(getRecordInfo(), e);
        }
    }

    private String getRecordInfo()
    {
        try
        {
            return String.format(
                    "Error processing record: id (%s), abon_id (%s), abon_number (%s)"
                    , currentRecord.getValue(ID_FIELD)
                    , currentRecord.getValue(ABONENT_ID_FIELD)
                    , currentRecord.getValue(ABONENT_NUMBER_FIELD));
        } catch (RecordException ex)
        {
            return "Error processing record";
        }
    }
        

    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        return null;
    }

    private void skipRecord() throws RecordException
    {
        currentRecord.setValue(COMPLETION_CODE_FIELD, SKIPPED_STATUS);
        Timestamp curTs = new Timestamp(System.currentTimeMillis());
        currentRecord.setValue(CALL_START_TIME_FIELD, curTs);
        currentRecord.setValue(CALL_END_TIME_FIELD, curTs);
    }

    private void informAbonent() throws Exception
    {
        recordLock.lock();
        try
        {
            conversationResult = null;
            String abonNumber = (String) currentRecord.getValue(ABONENT_NUMBER_FIELD);
            try{
                informerStatus = IvrInformerStatus.WAITING_FOR_ENDPOINT;
                endpoint.getEndpointState().waitForState(
                        new int[]{IvrEndpointState.IN_SERVICE}, Long.MAX_VALUE);
                informerStatus = IvrInformerStatus.PROCESSING;
                endpoint.invite(abonNumber, conversationScenario, this);
                recordProcessed.await();
                String status = null;
                if (conversationResult==null)
                    status = PROCESSING_ERROR_STATUS;
                else
                {
                    boolean sucProc = false;
                    switch(conversationResult.getCompletionCode())
                    {
                        case COMPLETED_BY_ENDPOINT: 
                            status = COMPLETED_BY_INFORMER_STATUS; sucProc = true; break;
                        case COMPLETED_BY_OPPONENT: 
                            status = COMPLETED_BY_ABONENT_STATUS; sucProc = true; break;
                        case OPPONENT_BUSY: status = NUMBER_BUSY_STATUS; break;
                        case OPPONENT_NO_ANSWERED: status = NUMBER_NOT_ANSWERED_STATUS; break;
                        case OPPONENT_UNKNOWN_ERROR: status = PROCESSING_ERROR_STATUS; break;
                    }
                    if (sucProc)
                        lastSuccessfullyProcessedAbonId =
                                (String) currentRecord.getValue(ABONENT_ID_FIELD);
                    currentRecord.setValue(COMPLETION_CODE_FIELD, status);
                    currentRecord.setValue(
                            CALL_START_TIME_FIELD, conversationResult.getCallStartTime());
                    currentRecord.setValue(
                            CALL_END_TIME_FIELD, conversationResult.getCallEndTime());
                    currentRecord.setValue(
                            CALL_DURATION_FIELD, conversationResult.getCallDuration());
                    currentRecord.setValue(
                            CONVERSATION_START_TIME_FIELD
                            , conversationResult.getConversationStartTime());
                    currentRecord.setValue(
                            CONVERSATION_DURATION_FIELD
                            , conversationResult.getConversationDuration());
                }
            }
            catch(IvrEndpointException ex)
            {
                currentRecord.setValue(COMPLETION_CODE_FIELD, PROCESSING_ERROR_STATUS);
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error(getRecordInfo(), ex);
            }
        }
        finally
        {
            recordLock.unlock();
        }
    }

    private void sendDataToConsumers(Object data)
    {
        Collection<Node> depNodes = getDependentNodes();
        if (depNodes!=null && !depNodes.isEmpty())
            for (Node dep: depNodes)
                if (dep instanceof DataConsumer && Status.STARTED.equals(dep.getStatus()))
                    ((DataConsumer)dep).setData(this, data);
    }
}
