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
package ch.quantasy.mqtt.gateway.client.contract;

import ch.quantasy.mqtt.gateway.client.message.Message;
import ch.quantasy.mqtt.gateway.client.message.Validator;
import ch.quantasy.mqtt.gateway.client.message.annotations.AValidator;
import ch.quantasy.mqtt.gateway.client.message.annotations.ArraySize;
import ch.quantasy.mqtt.gateway.client.message.annotations.ArraySizes;
import ch.quantasy.mqtt.gateway.client.message.annotations.Choice;
import ch.quantasy.mqtt.gateway.client.message.annotations.Default;
import ch.quantasy.mqtt.gateway.client.message.annotations.Fraction;
import ch.quantasy.mqtt.gateway.client.message.annotations.MultiArraySize;
import ch.quantasy.mqtt.gateway.client.message.annotations.Nullable;
import ch.quantasy.mqtt.gateway.client.message.annotations.Period;
import ch.quantasy.mqtt.gateway.client.message.annotations.Range;
import ch.quantasy.mqtt.gateway.client.message.annotations.Ranges;
import ch.quantasy.mqtt.gateway.client.message.annotations.SetSize;
import ch.quantasy.mqtt.gateway.client.message.annotations.StringForm;
import ch.quantasy.mqtt.gateway.client.message.annotations.StringSize;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author reto
 */
public abstract class AyamlServiceContract extends AServiceContract {

    private final ObjectMapper mapper;

    public AyamlServiceContract(String rootContext, String baseClass) {
        this(rootContext, baseClass, null);
    }

    public AyamlServiceContract(String rootContext, String baseClass, String instance) {
        super(rootContext, baseClass, instance);
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public ObjectMapper getObjectMapper() {
        return mapper;
    }

    public String toMD() {
        String toMD = "";
        Map<String, String> descriptions = new TreeMap<>();
        describe(descriptions);
        toMD += "### " + BASE_CLASS + "\n";
        for (Map.Entry<String, String> entry : descriptions.entrySet()) {
            String key = entry.getKey();
            String value = "   " + entry.getValue();
            value = value.replaceAll("\n", "\n   ");
            toMD += "```\n";
            toMD += key + "\n" + value + "\n";
            toMD += "```\n";
        }
        return toMD;
    }

    @Override
    protected void describe(Map<String, String> descriptions) {
        for (Map.Entry<String, Class<? extends Message>> entry : getMessageTopicMap().entrySet()) {
            descriptions.put(entry.getKey(), getDataFormatDescription(entry.getValue()));
        }
    }

    public static String getDataFormatDescription(Class o) {
        return getDataFormatDescription(o, "");
    }

    public static String getDataFormatDescription(Class o, String indentation) {
        String descriptionOptional = indentation + "optional: # this tag is not part of the data structure\n";
        String descriptionRequired = indentation + "required: # this tag is not part of the data structure\n";
        indentation += "  ";
        if (o == null) {
            return "";
        }
        List<Field> fields = new ArrayList();
        fields.addAll(Arrays.asList(o.getDeclaredFields()));
        Class<?> current = o;
        while (current.getSuperclass() != null && AValidator.class.isAssignableFrom(current.getSuperclass())) {
            current = current.getSuperclass();
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
        }
        Collections.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field o1, Field o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        boolean required = false;
        for (Field field : fields) {
            String endOfLine = "\n";
            try {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Default.class)) {
                    endOfLine = " # default: " + field.getAnnotation(Default.class).value() + "\n";
                }
                if (field.isAnnotationPresent(Nullable.class)) {
                    required = false;
                } else {
                    required = true;
                }
                if (field.isAnnotationPresent(ArraySize.class)) {
                    String desc = indentation;
                    desc += field.getName() + ": ";
                    desc += "Array <";
                    ArraySize annotation = field.getAnnotation(ArraySize.class);
                    desc += "min: " + annotation.min() + " ";
                    desc += "max: " + annotation.max();
                    desc += ">";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                }
                if (field.isAnnotationPresent(ArraySizes.class)) {
                    String desc = indentation;

                    desc += field.getName() + ": ";

                    desc += "Array <[";
                    ArraySizes annotation = field.getAnnotation(ArraySizes.class);
                    String separator = "";
                    for (ArraySize arraySize : annotation.values()) {
                        desc += separator;
                        desc += "min: " + arraySize.min() + " ";
                        desc += "max: " + arraySize.max();
                        separator = ",";
                    }
                    desc += "]>";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                }
                if (field.isAnnotationPresent(MultiArraySize.class)) {
                    String desc = indentation;
                    desc += field.getName() + ": ";
                    desc += "Arrays: <[";
                    MultiArraySize annotation = field.getAnnotation(MultiArraySize.class);
                    String separator = "";
                    for (ArraySize arraySize : annotation.values()) {
                        desc += separator;
                        desc += "min: " + arraySize.min() + " ";
                        desc += "max: " + arraySize.max();
                        separator = ",";
                    }
                    desc += "]>";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }

                }
                if (field.isAnnotationPresent(StringSize.class)) {
                    String desc = indentation;
                    desc += field.getName() + ": ";
                    desc += "String <";
                    StringSize annotation = field.getAnnotation(StringSize.class);
                    desc += "min: " + annotation.min() + " ";
                    desc += "max: " + annotation.max();
                    desc += ">";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                }
                if (field.isAnnotationPresent(Period.class)) {
                    String desc = indentation;

                    desc += field.getName() + ": ";
                    desc += "Number <";
                    Period annotation = field.getAnnotation(Period.class);
                    desc += "from: " + annotation.from() + " ";
                    desc += "to: " + annotation.to();
                    desc += ">";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                }
                if (field.isAnnotationPresent(Ranges.class)) {
                    String desc = indentation;

                    desc += field.getName() + ": ";

                    desc += "Number <[";
                    Ranges annotation = field.getAnnotation(Ranges.class);
                    String separator = "";
                    for (Range range : annotation.values()) {
                        desc += separator;
                        desc += "from: " + range.from() + " ";
                        desc += "to: " + range.to();
                        separator = ",";
                    }
                    desc += "]>";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                }
                if (field.isAnnotationPresent(Range.class)) {
                    String desc = indentation;

                    desc += field.getName() + ": ";

                    desc += "Number <";
                    Range annotation = field.getAnnotation(Range.class);
                    desc += "from: " + annotation.from() + " ";
                    desc += "to: " + annotation.to();
                    desc += ">";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                }
                if (field.isAnnotationPresent(Fraction.class)) {
                    String desc = indentation;

                    desc += field.getName() + ": ";

                    desc += "Fraction <";
                    Fraction annotation = field.getAnnotation(Fraction.class);
                    desc += "from: " + annotation.from() + " ";
                    desc += "to: " + annotation.to();
                    desc += ">";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }

                }
                if (field.isAnnotationPresent(Choice.class)) {
                    String desc = indentation;

                    desc += field.getName() + ": ";

                    desc += "String <";
                    Choice annotation = field.getAnnotation(Choice.class);
                    desc += Arrays.toString(annotation.values());
                    desc += ">";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                }

                if (field.isAnnotationPresent(SetSize.class)) {
                    String desc = indentation;

                    desc += field.getName() + ": ";

                    desc += "Set <";
                    SetSize annotation = field.getAnnotation(SetSize.class);

                    desc += "min: " + annotation.min() + " ";
                    desc += "max: " + annotation.max();
                    desc += ">";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                }
                if (field.isAnnotationPresent(StringForm.class)) {
                    String desc = indentation;
                    desc += field.getName() + ": ";
                    desc += "String <";
                    StringForm annotation = field.getAnnotation(StringForm.class);
                    desc += "regEx: " + annotation.regEx();
                    desc += ">";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                }

                Class c = field.getType();
                if (Boolean.class.isAssignableFrom(c) || boolean.class.isAssignableFrom(c)) {
                    String desc = indentation;
                    desc += field.getName() + ": ";
                    desc += "Boolean <true,false>";
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                } else if (Enum.class.isAssignableFrom(c)) {
                    String desc = enumDescription(field.getType(), field.getName(), indentation);
                    desc += endOfLine;
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                } else if (c != o && Validator.class.isAssignableFrom(c)) {
                    String desc = indentation;
                    desc += field.getName() + ":";
                    desc += endOfLine;
                    desc += getDataFormatDescription(c, indentation + "  ");
                    if (required) {
                        descriptionRequired += desc;
                    } else {
                        descriptionOptional += desc;
                    }
                } else if (c.isArray()) {
                    c = c.getComponentType();
                    if (Validator.class.isAssignableFrom(c)) {
                        String desc = indentation + "  " + c.getSimpleName() + ":";
                        desc += endOfLine;
                        desc += getDataFormatDescription(c, indentation + "    ");
                        if (required) {
                            descriptionRequired += desc;
                        } else {
                            descriptionOptional += desc;
                        }

                    }
                } else if (Collection.class.isAssignableFrom(c)) {
                    Type type = field.getGenericType();

                    if (type instanceof ParameterizedType) {

                        ParameterizedType pType = (ParameterizedType) type;
                        Type[] arr = pType.getActualTypeArguments();
                        for (Type tp : arr) {
                            Class<?> clzz = (Class<?>) tp;
                            String desc = indentation + field.getName() + ":";
                            desc += endOfLine;
                            desc += getDataFormatDescription(clzz, indentation + " ");
                            if (required) {
                                descriptionRequired += desc;
                            } else {
                                descriptionOptional += desc;
                            }
                        }
                    }

                }

            } catch (Exception ex) {
                Logger.getLogger(AValidator.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                field.setAccessible(false);
            }
        }
        if (descriptionOptional.trim().length() == "optional: # this tag is not part of the data structure".length()) {
            descriptionOptional = "";
        }

        if (descriptionRequired.trim().length() == "required: # this tag is not part of the data structure".length()) {
            descriptionRequired = "";
        }

        String delim = "";
        if (descriptionOptional.length() > 0 && descriptionRequired.length() > 0 && !descriptionRequired.endsWith("\n")) {
            delim = "\n";
        }
        return descriptionRequired + delim + descriptionOptional;
    }

    public static String enumDescription(Class enumType, String fieldName, String indentation) {
        String description = indentation;
        description += fieldName + ": String <";
        Field[] fields = enumType.getDeclaredFields();
        String separator = "";
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                if (field.isEnumConstant()) {
                    description += separator;
                    description += field.getName();
                    separator = ",";

                }

            } catch (Exception ex) {
                Logger.getLogger(AValidator.class
                        .getName()).log(Level.SEVERE, null, ex);
            } finally {
                field.setAccessible(false);
            }
        }
        description += ">";
        return description;
    }
}
