/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package com.heliosapm.tsdb.grapi;

/**
 * <p>Title: GraphiteAdapter</p>
 * <p>Description: Defines an adapter that accepts requests for data from the core plugin service 
 * and returns the results in the standard GraphiteAPI RPC format.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.GraphiteAdapter</code></p>
 */

public interface GraphiteAdapter {

}


/*
Example from a graphite loaded browser.
=======================================

Request:   http://localhost:8080/metrics/find/?query=tpmint.cpu-*

Response:

[
  {
    "leaf": 0,
    "context": {},
    "text": "cpu-0",
    "expandable": 1,
    "id": "tpmint.cpu-0",
    "allowChildren": 1
  },
  {
    "leaf": 0,
    "context": {},
    "text": "cpu-1",
    "expandable": 1,
    "id": "tpmint.cpu-1",
    "allowChildren": 1
  },
  {
    "leaf": 0,
    "context": {},
    "text": "cpu-2",
    "expandable": 1,
    "id": "tpmint.cpu-2",
    "allowChildren": 1
  },
  {
    "leaf": 0,
    "context": {},
    "text": "cpu-3",
    "expandable": 1,
    "id": "tpmint.cpu-3",
    "allowChildren": 1
  }
]

*/