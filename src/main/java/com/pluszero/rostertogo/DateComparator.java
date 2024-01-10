/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo;

import com.pluszero.rostertogo.model.PlanningEvent;
import java.util.Comparator;

/**
 *
 * @author Cyril
 */
public class DateComparator implements Comparator<PlanningEvent> {

    @Override
    public int compare(PlanningEvent o1, PlanningEvent o2) {
        return o1.getGcBegin().compareTo(o2.getGcBegin());
    }

}
