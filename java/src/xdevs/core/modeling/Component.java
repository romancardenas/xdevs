/*
 * Copyright (C) 2014-2015 José Luis Risco Martín <jlrisco@ucm.es> and 
 * Saurabh Mittal <smittal@duniptech.com>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see
 * http://www.gnu.org/licenses/
 *
 * Contributors:
 *  - José Luis Risco Martín
 */
package xdevs.core.modeling;

import java.util.ArrayList;
import java.util.Collection;

public abstract class Component {

    // Component attributes
    protected Component parent = null;
    protected String name;
    protected ArrayList<Port<?>> inPorts = new ArrayList<>();
    protected ArrayList<Port<?>> outPorts = new ArrayList<>();

    public Component(String name) {
        this.name = name;
    }

    public Component() {
        this(Component.class.getSimpleName());
    }
    
    public String getName() {
        return name;
    }
    
    public abstract void initialize();
    public abstract void exit();

    public boolean isInputEmpty() {
        for (Port<?> port : inPorts) {
            if (!port.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void addInPort(Port<?> port) {
        inPorts.add(port);
        port.parent = this;
    }

    public Collection<Port<?>> getInPorts() {
        return inPorts;
    }

    public void addOutPort(Port<?> port) {
        outPorts.add(port);
        port.parent = this;
    }

    public Collection<Port<?>> getOutPorts() {
        return outPorts;
    }

    public Component getParent() {
        return parent;
    }

    public void setParent(Component parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name + " :");
        sb.append(" Inports[ ");
        for (Port<?> p : inPorts) {
            sb.append(p).append(" ");
        }
        sb.append("]");
        sb.append(" Outports[ ");
        for (Port<?> p : outPorts) {
            sb.append(p).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }
}