/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.register.client.server.zookeeper;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.type.DataTypeParent;
import org.apache.shenyu.register.client.server.api.ShenyuClientServerRegisterPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for Zookeeper register center.
 */
public class ZookeeperServerRegisterRepositoryTest {
    
    private ZookeeperClientServerRegisterRepository repository;
    
    private ShenyuClientServerRegisterPublisher publisher;
    
    private IZkChildListener zkChildListener;
    
    private IZkDataListener zkDataListener;
    
    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        this.publisher = mockPublish();
        this.repository = new ZookeeperClientServerRegisterRepository();
        Class<? extends ZookeeperClientServerRegisterRepository> clazz = this.repository.getClass();
        
        String fieldClientString = "zkClient";
        Field fieldClient = clazz.getDeclaredField(fieldClientString);
        fieldClient.setAccessible(true);
        fieldClient.set(repository, mockZkClient());
        
        String fieldPublisherString = "publisher";
        Field fieldPublisher = clazz.getDeclaredField(fieldPublisherString);
        fieldPublisher.setAccessible(true);
        fieldPublisher.set(repository, publisher);
    }
    
    private ShenyuClientServerRegisterPublisher mockPublish() {
        ShenyuClientServerRegisterPublisher publisher = mock(ShenyuClientServerRegisterPublisher.class);
        doNothing().when(publisher).publish(localAny());
        return publisher;
    }
    
    private ZkClient mockZkClient() {
        MetaDataRegisterDTO data = MetaDataRegisterDTO.builder().build();
        ZkClient client = mock(ZkClient.class);
        
        when(client.getChildren(anyString())).thenReturn(Arrays.asList("/path1", "/path2"));
        when(client.readData(anyString())).thenReturn(GsonUtils.getInstance().toJson(data));
        
        doNothing().when(client).createPersistent(anyString(), anyBoolean());
        
        doAnswer(invocationOnMock -> {
            this.zkChildListener = invocationOnMock.getArgument(1);
            return null;
        }).when(client).subscribeChildChanges(anyString(), any(IZkChildListener.class));
        
        doAnswer(invocationOnMock -> {
            this.zkDataListener = invocationOnMock.getArgument(1);
            return null;
        }).when(client).subscribeDataChanges(anyString(), any(IZkDataListener.class));
        
        return client;
    }
    
    @Test
    public void testSubscribeMetaData() throws Exception {
        Class<? extends ZookeeperClientServerRegisterRepository> clazz = this.repository.getClass();
        String methodString = "subscribeMetaData";
        Method method = clazz.getDeclaredMethod(methodString, String.class);
        method.setAccessible(true);
        method.invoke(repository, "http");
        
        verify(publisher, times(4)).publish(localAny());
        
        zkChildListener.handleChildChange("/path", Arrays.asList("/path1", "/path2", "/path3"));
        verify(publisher, times(10)).publish(localAny());
        
        String data = GsonUtils.getInstance().toJson(MetaDataRegisterDTO.builder().build());
        zkDataListener.handleDataChange("/path1", data);
        verify(publisher, times(11)).publish(localAny());
    }
    
    @Test
    public void testSubscribeURI() throws Exception {
        Class<? extends ZookeeperClientServerRegisterRepository> clazz = this.repository.getClass();
        String methodString = "subscribeURI";
        Method method = clazz.getDeclaredMethod(methodString, String.class);
        method.setAccessible(true);
        method.invoke(repository, "http");
        
        verify(publisher, times(2)).publish(localAny());
        
        zkChildListener.handleChildChange("/path", Arrays.asList("/path1", "/path2", "/path3"));
        verify(publisher, times(5)).publish(localAny());
        
        zkChildListener.handleChildChange("/path", Collections.emptyList());
        verify(publisher, times(6)).publish(localAny());
    }
    
    private List<DataTypeParent> localAny() {
        return any();
    }
}
