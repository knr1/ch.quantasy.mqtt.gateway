/*
 * /*
 *  *   "SeMqWay"
 *  *
 *  *    SeMqWay(tm): A gateway to provide an MQTT-View for any micro-service (Service MQTT-Gateway).
 *  *
 *  *    Copyright (c) 2016 Bern University of Applied Sciences (BFH),
 *  *    Research Institute for Security in the Information Society (RISIS), Wireless Communications & Secure Internet of Things (WiCom & SIoT),
 *  *    Quellgasse 21, CH-2501 Biel, Switzerland
 *  *
 *  *    Licensed under Dual License consisting of:
 *  *    1. GNU Affero General Public License (AGPL) v3
 *  *    and
 *  *    2. Commercial license
 *  *
 *  *
 *  *    1. This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Affero General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Affero General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Affero General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *  *
 *  *    2. Licensees holding valid commercial licenses for TiMqWay may use this file in
 *  *     accordance with the commercial license agreement provided with the
 *  *     Software or, alternatively, in accordance with the terms contained in
 *  *     a written agreement between you and Bern University of Applied Sciences (BFH),
 *  *     Research Institute for Security in the Information Society (RISIS), Wireless Communications & Secure Internet of Things (WiCom & SIoT),
 *  *     Quellgasse 21, CH-2501 Biel, Switzerland.
 *  *
 *  *
 *  *     For further information contact <e-mail: reto.koenig@bfh.ch>
 *  *
 *  *
 */
package ch.quantasy.mqtt.gateway.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author reto
 */
public abstract class AClientContract {

    public final String ROOT_CONTEXT;
    public final String INSTANCE;
    public final String CANONICAL_TOPIC;
    public final String BASE_CLASS;
    public final String BASE_TOPIC;
    public final String STATUS;
    public final String STATUS_CONNECTION;
    public final String OFFLINE;
    public final String ONLINE;

    public final String EVENT;
    public final String INTENT;
    public final String DESCRIPTION;

    public AClientContract(String rootContext, String baseClass) {
        this(rootContext, baseClass, null);
    }

    public AClientContract(String rootContext, String baseClass, String instance) {
        ROOT_CONTEXT = rootContext;
        BASE_CLASS = baseClass;
        BASE_TOPIC = ROOT_CONTEXT + "/" + BASE_CLASS;
        INSTANCE = instance;
        if (INSTANCE != null) {
            CANONICAL_TOPIC = BASE_TOPIC + "/U/" + INSTANCE;
        } else {
            CANONICAL_TOPIC = BASE_TOPIC;
        }

        EVENT = CANONICAL_TOPIC + "/E";
        INTENT = CANONICAL_TOPIC + "/I";
        STATUS = CANONICAL_TOPIC + "/S";
        DESCRIPTION = BASE_TOPIC + "/D";

        STATUS_CONNECTION = STATUS + "/connection";
        OFFLINE = "offline";
        ONLINE = "online";
    }

    public void publishContracts(GatewayClient gatewayClient) {

        Map<String, String> descriptions = new HashMap<>();
        describe(descriptions);
        descriptions.put(STATUS_CONNECTION, "[" + ONLINE + "|" + OFFLINE + "]");

        for (Map.Entry<String, String> description : descriptions.entrySet()) {
            gatewayClient.publishDescription(description.getKey(), description.getValue());
        }
    }

    public abstract ObjectMapper getObjectMapper();

    protected abstract void describe(Map<String, String> descriptions);

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 73 * hash + Objects.hashCode(this.CANONICAL_TOPIC);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AClientContract other = (AClientContract) obj;
        if (!Objects.equals(this.CANONICAL_TOPIC, other.CANONICAL_TOPIC)) {
            return false;
        }
        return true;
    }
    
    

}
