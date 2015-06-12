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

import net.opentsdb.core.TSDB;
import net.opentsdb.stats.StatsCollector;
import net.opentsdb.tsd.RpcPlugin;
import net.opentsdb.utils.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Deferred;

/**
 * <p>Title: GraphiteAPIService</p>
 * <p>Description: OpenTSDB RpcPlugin that mimics the <a href="http://graphite.org">Graphite</a> server's HTTP API
 * as used by <a href="http://grafana.org">Grafana</a> but retrieves the data from elsewhere.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.GraphiteAPIService</code></p>
 */

public class GraphiteAPIService extends RpcPlugin {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The TSDB instance */
	protected TSDB tsdb = null;
	/** The TSDB's configuration */
	protected Config config = null;
	
	/**
	 * Creates a new GraphiteAPIService
	 */
	public GraphiteAPIService() {
		log.info("Created GraphiteAPIService RpcPlugin Instance");
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RpcPlugin#initialize(net.opentsdb.core.TSDB)
	 */
	@Override
	public void initialize(final TSDB tsdb) {
		this.tsdb = tsdb;
		config = tsdb.getConfig();
		
		log.info("GraphiteAPIService RpcPlugin Instance Initialized");
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RpcPlugin#shutdown()
	 */
	@Override
	public Deferred<Object> shutdown() {
		// TODO Implement generalized shutdown
		return Deferred.fromResult(null);
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RpcPlugin#version()
	 */
	@Override
	public String version() {
		return "2.1.0";
	}

	/**
	 * {@inheritDoc}
	 * @see net.opentsdb.tsd.RpcPlugin#collectStats(net.opentsdb.stats.StatsCollector)
	 */
	@Override
	public void collectStats(final StatsCollector collector) {
		// TODO Gather stats from all adapters

	}

}
