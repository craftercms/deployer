/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.deployer.impl;

import java.util.List;

import org.craftercms.deployer.api.Deployer;
import org.craftercms.deployer.api.EventListener;
import org.craftercms.deployer.api.SiteContext;
import org.craftercms.deployer.api.event.ErrorEvent;
import org.craftercms.deployer.api.event.Event;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Created by alfonsovasquez on 5/12/16.
 */
public class SiteContextImpl implements SiteContext {

    protected String name;
    protected Deployer deployer;
    protected List<EventListener> postDeployListeners;
    protected List<EventListener> errorListeners;
    protected ConfigurableApplicationContext applicationContext;

    public SiteContextImpl(String name, Deployer deployer, List<EventListener> postDeployListeners, List<EventListener> errorListeners,
                           ConfigurableApplicationContext applicationContext) {
        this.name = name;
        this.deployer = deployer;
        this.postDeployListeners = postDeployListeners;
        this.errorListeners = errorListeners;
        this.applicationContext = applicationContext;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Deployer getDeployer() {
        return deployer;
    }

    @Override
    public void fireEvent(Event event) {
        if (event instanceof ErrorEvent) {
            notifyListeners(event, errorListeners);
        } else {
            notifyListeners(event, postDeployListeners);
        }
    }

    @Override
    public long getDateCreated() {
        return applicationContext.getStartupDate();
    }

    @Override
    public void close() {
        applicationContext.close();
    }

    protected void notifyListeners(Event event, List<EventListener> listeners) {
        for (EventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

}
