/*
 *   Copyright 2009-2015 Markus Schaffner
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package coral.test;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import coral.model.ExpData;
import coral.service.ErrorFlag;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class TestErrorFlag {

    @Test
    public void testErrorFlag() {

        {
            ErrorFlag ef = new ErrorFlag(false);

            ExpData m = new ExpData();

            ef.validateJs("name1", " 1 == 1", m); // true
            // ef.validateJs("name2", " 1 != hoxenplo", m); // error
            ef.validateJs("name3", " 1 != 1", m); // false

            m.put("test1", 1);

            ef.validateJs("name4", " 1 == test1", m);

            m.put("price1", 10);
            m.put("price2", 1);

            ef.validateFormated(m,
                    "price1::1::11;price2::1::11;priceCond:(price1>=price2)"
                            .split(";"));

            for (Map.Entry<?, ?> entry : ef.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }

        // number (precision)
        {
            System.out.println();

            ErrorFlag ef = new ErrorFlag(false);
            ExpData m = new ExpData();

            m.put("test", "4.02"); // false
            m.put("test2", "4.02"); // true
            m.put("test3", "4"); // true
            m.put("test4", "12"); // false

            String[] props = new String[] { "test::1::10", "test2::1.000::10",
                    "test3::1::10", "test4::1::10" };

            ef.validateFormated(m, props);

            for (Map.Entry<?, ?> entry : ef.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }

        }

    }

    @Test
    public void testValidateEqualSet() {

        ExpData m = new ExpData();

        m.put("test", "hello"); // true
        m.put("test2", "holle"); // false

        {
            String[] props = new String[] { "test={hello::hallo::hullo}" };
            ErrorFlag ef = new ErrorFlag(false);
            ef.validateFormated(m, props);
            assertTrue(ef.isValid());
        }

        {
            String[] props2 = new String[] { "test2={hello::hallo::hullo}" };
            ErrorFlag ef2 = new ErrorFlag(false);
            ef2.validateFormated(m, props2);
            assertTrue(!ef2.isValid());
        }

    }

}
