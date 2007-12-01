/*
 * $HeadURL$
 * $Revision$
 * $Date$
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.impl.nio.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

/**
 * Basic tests for {@link DefaultListeningIOReactor}.
 *
 * @author <a href="mailto:oleg at ural.ru">Oleg Kalnichevski</a>
 * 
 * @version $Id$
 */
public class TestDefaultListeningIOReactor extends TestCase {

    // ------------------------------------------------------------ Constructor
    public TestDefaultListeningIOReactor(String testName) {
        super(testName);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String args[]) {
        String[] testCaseName = { TestDefaultListeningIOReactor.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    // ------------------------------------------------------- TestCase Methods

    public static Test suite() {
        return new TestSuite(TestDefaultListeningIOReactor.class);
    }
    
    public void testEndpointUpAndDown() throws Exception {
        
        HttpParams params = new BasicHttpParams();
        
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        final BufferingHttpServiceHandler serviceHandler = new BufferingHttpServiceHandler(
                httpproc,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(),
                params);
        
        final IOEventDispatch eventDispatch = new DefaultServerIOEventDispatch(
                serviceHandler, 
                params);
        
        final ListeningIOReactor ioreactor = new DefaultListeningIOReactor(1, params);
        
        Thread t = new Thread(new Runnable() {
            
            public void run() {
                try {
                    ioreactor.execute(eventDispatch);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
        });
        
        t.start();
        
        ListenerEndpoint[] endpoints = ioreactor.getEndpoints();
        assertNotNull(endpoints);
        assertEquals(0, endpoints.length);
        
        ListenerEndpoint port9998 = ioreactor.listen(new InetSocketAddress(9998));
        port9998.waitFor();

        ListenerEndpoint port9999 = ioreactor.listen(new InetSocketAddress(9999));
        port9999.waitFor();

        endpoints = ioreactor.getEndpoints();
        assertNotNull(endpoints);
        assertEquals(2, endpoints.length);
        
        port9998.close();

        endpoints = ioreactor.getEndpoints();
        assertNotNull(endpoints);
        assertEquals(1, endpoints.length);
        
        assertEquals(9999, ((InetSocketAddress) endpoints[0].getAddress()).getPort());
        
        ioreactor.shutdown(1000);
        t.join(1000);
        
        assertEquals(IOReactorStatus.SHUT_DOWN, ioreactor.getStatus());
    }

}
