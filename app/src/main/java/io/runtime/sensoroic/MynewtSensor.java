package io.runtime.sensoroic;

import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MynewtSensor {

    /* Logging tag */
    public final static String TAG = "MynewtSensor";

    /* OIC resource types for sensors exposed over the Mynewt sensor framwork */
    public final static String MYNEWT_SENSOR_RT_PREFIX = "x.mynewt.snsr.";
    public final static String RT_LINEAR_ACCELEROMETER = MYNEWT_SENSOR_RT_PREFIX + "lacc";
    public final static String RT_ACCELEROMETER = MYNEWT_SENSOR_RT_PREFIX + "acc";
    public final static String RT_MAGNETOMETER = MYNEWT_SENSOR_RT_PREFIX + "mag";
    public final static String RT_LIGHT_SENSOR = MYNEWT_SENSOR_RT_PREFIX + "lt";
    public final static String RT_TEMPERATURE_SENSOR = MYNEWT_SENSOR_RT_PREFIX + "tmp";
    public final static String RT_AMBIENT_TEMPERATURE_SENSOR = MYNEWT_SENSOR_RT_PREFIX + "ambtmp";
    public final static String RT_RELATIVE_HUMIDITY_SENSOR = MYNEWT_SENSOR_RT_PREFIX + "rhmty";
    public final static String RT_PRESSURE_SENSOR = MYNEWT_SENSOR_RT_PREFIX + "psr";
    public final static String RT_COLOR_SENSOR = MYNEWT_SENSOR_RT_PREFIX + "col";
    public final static String RT_GYROSCOPE = MYNEWT_SENSOR_RT_PREFIX + "gyr";
    public final static String RT_EULER = MYNEWT_SENSOR_RT_PREFIX + "eul";
    public final static String RT_GRAVITY = MYNEWT_SENSOR_RT_PREFIX + "grav";
    public final static String RT_ROTATION_VECTOR = MYNEWT_SENSOR_RT_PREFIX + "quat";

    /* Human readable names for each sensor type */
    public final static String TITLE_LINEAR_ACCELEROMETER = "Linear Accelerometer";
    public final static String TITLE_ACCELEROMETER = "Accelerometer";
    public final static String TITLE_MAGNETOMETER = "Magnetometer";
    public final static String TITLE_LIGHT_SENSOR = "Light Sensor";
    public final static String TITLE_TEMPERATURE_SENSOR = "Temperature Sensor";
    public final static String TITLE_AMBIENT_TEMPERATURE_SENSOR = "Ambient Temperature Sensor";
    public final static String TITLE_RELATIVE_HUMIDITY_SENSOR = "Relative Humidity Sensor";
    public final static String TITLE_PRESSURE_SENSOR = "Pressure Sensor";
    public final static String TITLE_COLOR_SENSOR = "Color Sensor";
    public final static String TITLE_GYROSCOPE = "Gyroscope";
    public final static String TITLE_EULER = "Euler Sensor";
    public final static String TITLE_GRAVITY = "Gravity Sensor";
    public final static String TITLE_ROTATION_VECTOR = "Rotation Vector (Quaternion)";

    /* Internal OcRepresentation of the Mynewt sensor */
    private OcRepresentation mSensorRepresentation;

    /* Sensor resource type string */
    private String mSensorType;

    /* The map of values obtained by the OCRepresentation */
    private HashMap<String, Object> mValues;

    /* The ordered map of sensor data values obtained from mValues. This data does not
     * contain the time information originally in the sensor response and is primarily used
     * for charting the data. */
    private LinkedHashMap<String, Object> mSensorData;

    // TODO OcRepresentation does not expose resource types, FIX in iotivity

    /**
     * MynewtSensor constructor to be used after observing/getting values from the OcResource.
     * @param ocRepresentation  The OC representation obtained from a observe/get callback.
     * @param sensorType        The Mynewt sensor type string obtained after discovery.
     */
    public MynewtSensor(OcRepresentation ocRepresentation, String sensorType) {
        mSensorRepresentation = ocRepresentation;
        mSensorType = sensorType;
        mValues = new HashMap<>(ocRepresentation.getValues());
        mSensorData = getSensorDataFromValues(mValues, mSensorType);
    }

    /**
     * Updates the internal representation and values of the sensor.
     * @param representation    The new OcRepresentation obtained from a observe/get callback.
     */
    public void updateSensor(OcRepresentation representation) {
        if (representation == null) {
            return;
        }
        mSensorRepresentation = representation;
        mValues.putAll(representation.getValues());
        mSensorData = getSensorDataFromValues(mValues, mSensorType);
    }

    /**
     * Get the number of values for this sensor. This includes time values.
     * @return The number of values for this sensor, including time values.
     */
    public int getValueCount() {
        return mValues.size();
    }

    /**
     * Get the number of sensor data points for this sensor. This does not include time values.
     * @return The number of values for this sensor, excluding time values.
     */
    public int getSensorDataCount() {
        return mSensorData.size();
    }

    /**
     * Get an ordered map of the most recent sensor data, excluding time values.
     * @return ordered map of sensor data
     */
    public LinkedHashMap<String, Object> getSensorData() {
        return mSensorData;
    }

    /**
     * Get an unordered map of the most recent sensor data, including time values.
     * @return unordered map of sensor data
     */
    public Map<String, Object> getValues() {
        return mSensorData;
    }

    /**
     * Get an ordered List of sensor data keys which matches getSensorDataValueList().
     * The sensor data keys exclude time keys.
     * @return ordered List of sensor data keys, excluding time keys.
     */
    public Set<String> getSensorDataKeySet() {
        return mSensorData.keySet();
    }

    /**
     * Get an ordered List of sensor data values which matches getSensorDataKeyList().
     * The sensor data values exclude time values.
     * @return ordered List of sensor data values, excluding time values.
     */
    public Collection<Object> getSensorDataValueList() {
        return mSensorData.values();
    }

    /**
     * Get the running time for the sensor as a double with microseconds.
     * @return the running time in seconds and microseconds
     */
    public double getRunningTime() {
        return Double.valueOf(String.valueOf(getRunningTimeSeconds()) + "." +
                              String.valueOf(mValues.get("ts_usecs")));
    }

    /**
     * Get the number of seconds the Mynewt sensor has been running.
     * @return sensor running time in seconds.
     */
    public int getRunningTimeSeconds() {
        return (Integer)mValues.get("ts_secs");
    }

    /**
     *
     * @return
     */
    public int getCpuTime() {
        return (Integer)mValues.get("ts_cputime");
    }

    //********************************************************
    // Static Utils
    //********************************************************

    /**
     * Determine whether an OcResource is a valid Mynewt sensor by checking if the resource type
     * list contains a valid mynwet sensor type
     * @param resource the resource to check
     * @return true if the resource is a valid Mynewt sensor, false otherwise
     */
    public static boolean isMynewtSensor(OcResource resource) {
        String rt = getSensorResourceType(resource.getResourceTypes());
        if (rt == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Get the Mynewt sensor specific resource type for a list of resource types.
     * Mynewt sensor resource types are of the form "x.myewt.snsr.*".
     * @param resourceTypes A list of resource types obtained from a OcResource object.
     * @return The sensor resource type if one exists in the list or null otherwise.
     */
    public static String getSensorResourceType(List<String> resourceTypes) {
        for (String rt : resourceTypes) {
            if (rt.startsWith(MYNEWT_SENSOR_RT_PREFIX)) {
                return rt;
            }
        }
        return null;
    }

    /**
     * Gets a human readable resource name for the Mynewt sensor from a OcResource object. If the resource is
     * not a valid mynewt sensor, the resource uri is returned instead.
     * @param resource the resource to obtain a human readable name for.
     * @return the human readable name
     */
    public static String getReadableName(OcResource resource) {
        String sensorType = getSensorResourceType(resource.getResourceTypes());
        if (sensorType == null) {
            return resource.getUri();
        }
        String readableString;
        switch (sensorType) {
            case RT_LINEAR_ACCELEROMETER:
                readableString = TITLE_LINEAR_ACCELEROMETER;
                break;
            case RT_ACCELEROMETER:
                readableString = TITLE_ACCELEROMETER;
                break;
            case RT_MAGNETOMETER:
                readableString = TITLE_MAGNETOMETER;
                break;
            case RT_LIGHT_SENSOR:
                readableString = TITLE_LIGHT_SENSOR;
                break;
            case RT_TEMPERATURE_SENSOR:
                readableString = TITLE_TEMPERATURE_SENSOR;
                break;
            case RT_AMBIENT_TEMPERATURE_SENSOR:
                readableString = TITLE_AMBIENT_TEMPERATURE_SENSOR;
                break;
            case RT_RELATIVE_HUMIDITY_SENSOR:
                readableString = TITLE_RELATIVE_HUMIDITY_SENSOR;
                break;
            case RT_PRESSURE_SENSOR:
                readableString = TITLE_PRESSURE_SENSOR;
                break;
            case RT_COLOR_SENSOR:
                readableString = TITLE_COLOR_SENSOR;
                break;
            case RT_GYROSCOPE:
                readableString = TITLE_GYROSCOPE;
                break;
            case RT_EULER:
                readableString = TITLE_EULER;
                break;
            case RT_GRAVITY:
                readableString = TITLE_GRAVITY;
                break;
            case RT_ROTATION_VECTOR:
                readableString = TITLE_ROTATION_VECTOR;
                break;
            default:
                readableString = resource.getUri();
                break;
        }
        return readableString;
    }

    /**
     * Get the data values from list of values and a sensor type. This method determines the order
     * in which the sensor data values are listed and does not include any time values from the
     * original values list.
     * @param values a map of sensor values
     * @param sensorType the sensor resource type corresponding to the list of values
     * @return an ordered map (LinkedHashMap) of sensor data, excluding time values.
     */
    private static LinkedHashMap<String, Object> getSensorDataFromValues(Map<String, Object> values, String sensorType) {
        LinkedHashMap<String, Object> returnValues = new LinkedHashMap<>();
        switch (sensorType) {
            case RT_LINEAR_ACCELEROMETER:
                returnValues.put("x", values.get("x"));
                returnValues.put("y", values.get("y"));
                returnValues.put("z", values.get("z"));
                break;
            case RT_ACCELEROMETER:
                returnValues.put("x", values.get("x"));
                returnValues.put("y", values.get("y"));
                returnValues.put("z", values.get("z"));
                break;
            case RT_MAGNETOMETER:
                returnValues.put("x", values.get("x"));
                returnValues.put("y", values.get("y"));
                returnValues.put("z", values.get("z"));
                break;
            case RT_LIGHT_SENSOR:
                returnValues.put("lux", values.get("lux"));
                returnValues.put("ir", values.get("ir"));
                returnValues.put("full", values.get("full"));
                break;
            case RT_TEMPERATURE_SENSOR:
                returnValues.put("temp", values.get("temp"));
                break;
            case RT_AMBIENT_TEMPERATURE_SENSOR:
                returnValues.put("temp", values.get("temp"));
                break;
            case RT_RELATIVE_HUMIDITY_SENSOR:
                returnValues.put("humid", values.get("humid"));
                break;
            case RT_PRESSURE_SENSOR:
                returnValues.put("press", values.get("press"));
                break;
            case RT_COLOR_SENSOR:
                returnValues.put("r", values.get("r"));
                returnValues.put("g", values.get("g"));
                returnValues.put("b", values.get("b"));
//                returnValues.put("c", values.get("c"));
                returnValues.put("lux", values.get("lux"));
                returnValues.put("ir", values.get("ir"));
                returnValues.put("colortemp", values.get("colortemp"));
                returnValues.put("cratio", values.get("cratio"));
                returnValues.put("saturation", values.get("saturation"));
                returnValues.put("is_sat", values.get("is_sat"));
                break;
            case RT_GYROSCOPE:
                returnValues.put("x", values.get("x"));
                returnValues.put("y", values.get("y"));
                returnValues.put("z", values.get("z"));
                break;
            case RT_EULER:
                returnValues.put("h", values.get("h"));
                returnValues.put("r", values.get("r"));
                returnValues.put("p", values.get("p"));
                break;
            case RT_GRAVITY:
                returnValues.put("x", values.get("x"));
                returnValues.put("y", values.get("y"));
                returnValues.put("z", values.get("z"));
                break;
            case RT_ROTATION_VECTOR:
                returnValues.put("x", values.get("x"));
                returnValues.put("y", values.get("y"));
                returnValues.put("z", values.get("z"));
                returnValues.put("w", values.get("w"));
                break;
            default:
                break;
        }
        return returnValues;
    }
}
