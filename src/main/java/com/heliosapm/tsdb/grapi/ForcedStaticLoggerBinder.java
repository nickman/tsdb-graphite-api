/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2015, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.tsdb.grapi;

import org.slf4j.ILoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.slf4j.spi.LoggerFactoryBinder;

import ch.qos.logback.classic.LoggerContext;

/**
 * <p>Title: ForcedStaticLoggerBinder</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdb.grapi.ForcedStaticLoggerBinder</code></p>
 */

public class ForcedStaticLoggerBinder implements LoggerFactoryBinder {
	private static final ForcedStaticLoggerBinder SINGLETON = new ForcedStaticLoggerBinder();
	public static String REQUESTED_API_VERSION = "1.6";
	
	public static final ForcedStaticLoggerBinder getSingleton() {
	      return SINGLETON;
	}
	/**
	 * Creates a new ForcedStaticLoggerBinder
	 */
	private ForcedStaticLoggerBinder() {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see org.slf4j.spi.LoggerFactoryBinder#getLoggerFactory()
	 */
	@Override
	public ILoggerFactory getLoggerFactory() {
		return new LoggerContext();
	}

	/**
	 * {@inheritDoc}
	 * @see org.slf4j.spi.LoggerFactoryBinder#getLoggerFactoryClassStr()
	 */
	@Override
	public String getLoggerFactoryClassStr() {
		return LoggerContext.class.getName();
	}

}
