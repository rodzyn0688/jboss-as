/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
 */
package org.jboss.as.domain.controller.plan;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;

/**
 * {@link ServerUpdateTask} that performs the update by triggering a
 * restart of the server. The restart results in the server getting the current
 * model state.
 */
class ServerRestartTask extends ServerUpdateTask {

    private final long gracefulTimeout;

    ServerRestartTask(final ServerIdentity serverId,
                      final ServerUpdatePolicy updatePolicy,
                      final ServerUpdateResultHandler resultHandler,
                      final long gracefulTimeout) {
        super(serverId, updatePolicy, resultHandler);
        this.gracefulTimeout = gracefulTimeout;
    }

    @Override
    public ModelNode getOperation() {
        return getRestartOp();
    }

    private ModelNode getRestartOp() {
        ModelNode address = new ModelNode();
        address.add(HOST, serverId.getHostName());

        ModelNode op = Util.getEmptyOperation("restart-server", address);
        op.get(SERVER).set(serverId.getServerName());
        op.get("graceful-timeout").set(gracefulTimeout);
        return op;
    }
}
