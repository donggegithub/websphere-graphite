/**
 * This file is part of Wasagent.
 *
 * Wasagent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wasagent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wasagent. If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package net.wait4it.graphite.wasagent.tests;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ibm.websphere.pmi.stat.WSStats;
import com.ibm.websphere.pmi.stat.WSTimeStatistic;
import com.ibm.websphere.pmi.stat.WSWebAppStats;

import net.wait4it.graphite.wasagent.core.WASClientProxy;

/**
 * Gets servlet service method execution time (ms).
 * 
 * The metric name is of the form:
 * 
 *   app_name#web_module_name.servlet_name
 * 
 * @author Yann Lambret
 *
 */
public class ServletTest extends TestUtils implements Test {

    // Servlet response time format
    private static final DecimalFormat DF = new DecimalFormat("0.00");

    /**
     * WebSphere servlets stats.
     * 
     * @param proxy   an applicative proxy for the target WAS instance
     * @param params  a comma separated list of servlet names, or
     *                a wildcard character (*) for all servlets
     * @return output a list of strings for collected data
     */
    public List<String> run(WASClientProxy proxy, String params) {
        // HTTP query params
        List<String> servlets = Arrays.asList(params.split(","));

        // Graphite data
        List<String> output = new ArrayList<String>();

        // Archive, servlet and metric names
        String archiveName, servletName, name;

        // PMI stats
        WSStats stats;
        WSTimeStatistic st;

        // Performance data
        double serviceTime;

        try {
            stats = proxy.getStats(WSWebAppStats.NAME);
        } catch (Exception ignored) {
            return output;
        }

        WSStats[] stats1 = stats.getSubStats(); // WEB module level
        for (WSStats stat1 : stats1) {
            archiveName = normalize(stat1.getName());
            WSStats[] stats2 = stat1.getSubStats(); // Servlets module level
            for (WSStats stat2 : stats2) {
                WSStats[] stats3 = stat2.getSubStats(); // Servlet level
                for (WSStats stat3 : stats3) {

                    // No statistics for WAS internal components
                    if (stat3.getName().matches("rspservlet")) {
                        continue;
                    }

                    if (servlets.contains("*") || servlets.contains(stat3.getName())) {
                        st = (WSTimeStatistic)stat3.getStatistic(WSWebAppStats.ServletStats.ServiceTime);
                        try {
                            serviceTime = st.getMean();
                        } catch (NullPointerException e) {
                            throw new RuntimeException("invalid 'Web Applications' PMI settings.");
                        }

                        servletName = normalize(stat3.getName());
                        name = archiveName + "." + servletName;
                        output.add("servlet." + name + ".serviceTime " + DF.format(serviceTime));
                    }
                }
            }
        }

        Collections.sort(output);
        return output;
    }

}
