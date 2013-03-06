/* This file is part of VoltDB.
 * Copyright (C) 2008-2013 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb;

import org.json_voltpatches.JSONObject;
import org.voltcore.messaging.HostMessenger;
import org.voltdb.rejoin.RejoinCoordinator;

import java.util.List;

public abstract class Joiner extends RejoinCoordinator {
    public Joiner(HostMessenger hostMessenger)
    {
        super(hostMessenger);
    }

    public abstract List<Integer> getPartitionsToAdd();
    public abstract JSONObject getTopology();
    public abstract void setClientInterface(ClientInterface ci);
    public abstract void setSites(List<Long> sites);
}
