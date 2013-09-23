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

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.pmi.stat.WSCountStatistic;
import com.ibm.websphere.pmi.stat.WSJTAStats;
import com.ibm.websphere.pmi.stat.WSStats;

import net.wait4it.graphite.wasagent.core.WASClientProxy;

/**
 * Gets the current transaction active count.
 * 
 * @author Yann Lambret
 *
 */
public class JTATest extends TestUtils implements Test {

    /**
     * WebSphere JTA stats.
     * 
     * @param proxy   an applicative proxy for the target WAS instance
     * @param params  null for this test
     * @return output a list of strings for collected data
     */
    public List<String> run(WASClientProxy proxy, String params) {
        // Graphite data
        List<String> output = new ArrayList<String>();

        // PMI stats
        WSStats stats;
        WSCountStatistic ac;

        // Performance data
        long activeCount;

        try {
            stats = proxy.getStats(WSJTAStats.NAME);
        } catch (Exception ignored) {
            return output;
        }

        ac = (WSCountStatistic)stats.getStatistic(WSJTAStats.ActiveCount);

        try {
            activeCount = ac.getCount();
        } catch (NullPointerException e) {
            throw new RuntimeException("invalid 'Transaction Manager' PMI settings.");
        }

        output.add("jta.activeCount " + activeCount);

        return output;
    }

}
