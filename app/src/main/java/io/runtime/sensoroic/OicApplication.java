package io.runtime.sensoroic;

import android.app.Application;

import com.github.mikephil.charting.data.Entry;

import org.iotivity.base.OcResource;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

public class OicApplication extends Application {

    /**
     * The global Hashtable of discovered resources is used throughout the app the get OcResource
     * object based a key which is the string concatination of the host address and the resource
     * uri (e.g. coap+tcp://XX:XX:XX:XX:XX:XX/resource/uri).
     */
    private Hashtable<String, OcResource> mDiscovered = new Hashtable<>();

    /**
     * Get the table of discovered resources.
     * @return the discovered resources
     */
    public Hashtable<String, OcResource> getDiscovered() {
        return mDiscovered;
    }

    /**
     * Put a resource into the table of discovered resources. If the resource already exists,
     * update the resource object value and return the replaced resource. If the resource did not
     * previously exist, add the entry and return null.
     * @param res The resource to add to the discovered table
     * @return If the entry already exists, return the replaced resource. Otherwise return null;
     */
    public OcResource putResource(OcResource res) {
        return mDiscovered.put(createUniqueId(res.getHost(), res.getUri()), res);
    }

    /**
     * Get a resource from the table of discovered resources given the unique id string for the
     * wanted resource. A unique id string is the concatenation of the resource device address
     * and the resource uri (e.g. coap+tcp://XX:XX:XX:XX:XX:XX/resource/uri).
     * @param uniqueId the string concatenation of the resource device address and the resource uri
     * @return returns the OcResource Object if the key exists, or null otherwise.
     */
    public OcResource getResource(String uniqueId) {
        return mDiscovered.get(uniqueId);
    }

    /**
     * Create a unique id for a resource given a host address and a resource uri. The unique id
     * is just the string concatenation of the two (e.g. coap+tcp://XX:XX:XX:XX:XX:XX/resource/uri).
     * @param hostAddr host address of the resource
     * @param resUri uri of the resource
     * @return the unique id for the resource
     */
    public static String createUniqueId(String hostAddr, String resUri) {
        return hostAddr + resUri;
    }

    /**
     * Create a unique id for a resource given an OcResource Object. The unique id is just the
     * string concatenation of the resource's host address and uri
     * (e.g. coap+tcp://XX:XX:XX:XX:XX:XX/resource/uri).
     * @param resource the resource to get the unique string for
     * @return the unique string
     */
    public static String createUniqueId(OcResource resource) {
        return resource.getHost() + resource.getUri();
    }

    /**
     * Historical Data
     * // TODO finish implementing historical data
     */

    private ArrayList<Entry> mHistoricalData = new ArrayList<>();

    public ArrayList<Entry> getHistoricalData() {
        return mHistoricalData;
    }

    public void addHistoricalData(Entry e) {
        mHistoricalData.add(e);
    }
}
