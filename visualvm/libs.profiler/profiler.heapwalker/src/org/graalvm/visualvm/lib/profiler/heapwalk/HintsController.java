/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.graalvm.visualvm.lib.profiler.heapwalk;

import java.awt.Color;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.graalvm.visualvm.lib.profiler.heapwalk.model.BrowserUtils;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.HintsControllerUI;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.openide.util.NbBundle;


/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HintsController_NoData=&lt;No Data&gt;",
    "HintsController_ClassName=Class Name",
    "HintsController_RetainedSize=Retained Size",
    "HintsController_ApplicationWindowsCaption=Application windows:"
})
public class HintsController extends AbstractController {
    
    private static final String CLASS_URL_PREFIX = "file://class/"; // NOI18N
    private static final String INSTANCE_URL_PREFIX = "file://instance/";   // NOI18N
        
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    
    private SummaryController summaryController;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    public HintsController(SummaryController summaryController) {
        this.summaryController = summaryController;
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public SummaryController getSummaryController() {
        return summaryController;
    }
    // --- Internal interface ----------------------------------------------------
    
    public void createNavigationHistoryPoint() {
        summaryController.getHeapFragmentWalker().createNavigationHistoryPoint();
    }
    
    public void showURL(final URL url) {
        BrowserUtils.performTask(new Runnable() {
            public void run() {
                String urls = url.toString();
                final HeapFragmentWalker heapFragmentWalker = summaryController.getHeapFragmentWalker();

                if (urls.startsWith(INSTANCE_URL_PREFIX)) {
                    urls = urls.substring(INSTANCE_URL_PREFIX.length());

                    final String[] id = urls.split("/"); // NOI18N
                    long instanceId = Long.parseLong(id[2]);
                    final Instance i = heapFragmentWalker.getHeapFragment().getInstanceByID(instanceId);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (i != null) {
                                heapFragmentWalker.getClassesController().showInstance(i);
                            } else {
                                ProfilerDialogs.displayError(Bundle.AnalysisController_CannotResolveInstanceMsg(id[1], id[0]));
                            }
                        }
                    });
                } else if (urls.startsWith(CLASS_URL_PREFIX)) {
                    urls = urls.substring(CLASS_URL_PREFIX.length());

                    final String[] id = urls.split("/"); // NOI18N
                    long jclsId = Long.parseLong(id[1]);
                    final JavaClass c = heapFragmentWalker.getHeapFragment().getJavaClassByID(jclsId);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (c != null) {
                                heapFragmentWalker.getClassesController().showClass(c);
                            } else {
                                ProfilerDialogs.displayError(Bundle.AnalysisController_CannotResolveClassMsg(id[0]));
                            }
                        }
                    });
                }
            }
        });
    }

    public void computeBiggestObjects(final int number) {
        BrowserUtils.performTask(new Runnable() {
            public void run() {
                int retainedSizesState = getSummaryController().getHeapFragmentWalker().
                                         computeRetainedSizes(true, true);

                final String result = retainedSizesState == HeapFragmentWalker.
                                      RETAINED_SIZES_COMPUTED ?
                                      findBiggestObjects(number) : Bundle.HintsController_NoData();

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ((HintsControllerUI)getPanel()).setResult(result);
                    }
                });
            }
        });
    }
    
    private String findBiggestObjects(int number) {
        Heap heap = getSummaryController().getHeapFragmentWalker().getHeapFragment();
        List<Instance> bigObjects = heap.getBiggestObjectsByRetainedSize(number);
        StringBuffer output = new StringBuffer();
        JavaClass java_lang_Class = heap.getJavaClassByName(Class.class.getName());
        NumberFormat formatter =  NumberFormat.getInstance();
        boolean oddRow = false;
        Color oddRowBackground = UIUtils.getDarker(
                        UIUtils.getProfilerResultsBackground());
        final String oddRowBackgroundString =
                "rgb(" + oddRowBackground.getRed() + "," + //NOI18N
                         oddRowBackground.getGreen() + "," + //NOI18N
                         oddRowBackground.getBlue() + ")"; //NOI18N
        
        output.append("<table border='0' width='100%'>");  // NOI18N
        output.append("<tr style='background-color:");  // NOI18N
        output.append(oddRowBackgroundString).append(";'>");  // NOI18N
        addHeading(output, Bundle.HintsController_ClassName());
        addHeading(output, Bundle.HintsController_RetainedSize());
        output.append("</tr>"); // NOI18N
        for(Instance in : bigObjects) {
            output.append(oddRow ? "<tr style='background-color: " + // NOI18N
                                      oddRowBackgroundString + ";'>" :  // NOI18N
                                      "<tr>");  // NOI18N
            if (in.getJavaClass().equals(java_lang_Class)) {
                JavaClass javaClass = heap.getJavaClassByID(in.getInstanceId());
                addCell(output,printClass(javaClass), false);
            } else {
                addCell(output,printInstance(in), false);
            }
            addCell(output,formatter.format(in.getRetainedSize()), true);
            output.append("</tr>");   // NOI18N
            oddRow = !oddRow;
        }
        output.append("</table>");   // NOI18N
        return output.toString();
    }
    
    // --- Private implementation ------------------------------------------------
    
    protected AbstractButton createControllerPresenter() {
        return ((HintsControllerUI) getPanel()).getPresenter();
    }
    
    private void addHeading(StringBuffer output,String text) {
        addTag(output,text,"th", false);   // NOI18N
    }

    private void addCell(StringBuffer output,String text, boolean ralign) {
        addTag(output,text,"td", ralign);   // NOI18N
    }

    private void addTag(StringBuffer output,String text,String tag, boolean ralign) {
        output.append("<").append(tag).append(ralign ? " style='text-align: right;'>" : ">");   // NOI18N
        output.append(text);
        output.append("</").append(tag).append(">");   // NOI18N
    }
    
    private String printInstance(Instance in) {
        String className = in.getJavaClass().getName();
        return "<a href='" + INSTANCE_URL_PREFIX + className + "/" + in.getInstanceNumber() + "/" + in.getInstanceId() + "'>" + className + '#' + in.getInstanceNumber() + "</a>"; // NOI18N
    }
    
    private String printClass(JavaClass jcls) {
        String className = jcls.getName();
        return "<a href='" + CLASS_URL_PREFIX + className + "/" + jcls.getJavaClassId() + "'>class " + className + "</a>"; // NOI18N
    }
    
    private String printWindow(Instance window, Heap heap) {
        boolean visible = DetailsUtils.getBooleanFieldValue(window, "visible", false); // NOI18N
        if (!visible) return null;
        
        String string = "&nbsp;&nbsp;&nbsp;&nbsp;"; // NOI18N
        string += printInstance(window);
        String details = DetailsUtils.getInstanceString(window, heap);
        if (details != null) string += " - " + details; // NOI18N
        
        return string;
    }
    
    private void populateUIPreview() {
        boolean hasWindows = false;
        StringBuilder sb = new StringBuilder();
        String windowsRes = Icons.getResource(HeapWalkerIcons.WINDOW);
        String hintsCaption = new String("<b><img border='0' align='bottom' src='nbresloc:/" + // NOI18N
                windowsRes + "'>&nbsp;&nbsp;" + Bundle.HintsController_ApplicationWindowsCaption() + "</b><br><hr>"); // NOI18N
        sb.append(hintsCaption);
        
        Heap heap = getSummaryController().getHeapFragmentWalker().getHeapFragment();
        JavaClass frameClass = heap.getJavaClassByName("java.awt.Frame"); // NOI18N
        if (frameClass != null) {
            Collection<JavaClass> frames = frameClass.getSubClasses();
            for (JavaClass frame : frames) {
                for (Instance f : (List<Instance>)frame.getInstances()) {
                    String string = printWindow(f, heap);
                    if (string != null) {
                        sb.append(string);
                        sb.append("<br>"); // NOI18N
                        hasWindows = true;
                    }
                }
            }
        }
        frameClass = heap.getJavaClassByName("java.awt.Dialog"); // NOI18N
        if (frameClass != null) {
            Collection<JavaClass> frames = frameClass.getSubClasses();
            for (JavaClass frame : frames) {
                for (Instance f : (List<Instance>)frame.getInstances()) {
                    String string = printWindow(f, heap);
                    if (string != null) {
                        sb.append(string);
                        sb.append("<br>"); // NOI18N
                        hasWindows = true;
                    }
                }
            }
        }
        
        if (!hasWindows) return;
        
        sb.append("<br>"); // NOI18N
        
        final String s = sb.toString();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { ((HintsControllerUI)getPanel()).setComponents(s); }
        });
    }
    
    // --- Protected implementation ----------------------------------------------
    protected JPanel createControllerUI() {
        HintsControllerUI ui = new HintsControllerUI(this);
        BrowserUtils.performTask(new Runnable() {
            public void run() { populateUIPreview(); }
        });
        return ui;
    }
    
}
