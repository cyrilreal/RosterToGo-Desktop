/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo;

import com.pluszero.rostertogo.model.PlanningEvent;
import java.util.ArrayList;

/**
 *
 * @author Cyril
 */
public class XmlWriter {

    ArrayList<PlanningEvent> list;

    public XmlWriter(ArrayList<PlanningEvent> list, String trigraph) {
        generateContent(list, trigraph);
    }

    private void generateContent(ArrayList<PlanningEvent> list, String trigraph) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<planning trigraph=\"").append(trigraph).append("\"");
        for (PlanningEvent planningEvent : list) {

        }
    }
}

//<?xml version="1.0" encoding="UTF-8"?>
//<planning trigraph="XXX" date_span="ddMMyyyy-ddMMyyyy">
//	<event>
//		<category>
//		<begin>
//		<end>
//		<label>
//		<iata_orig>
//		<iata_dest>
//		<lag_dest>
//		<flt_number>
//		<crew>
//		<training>
//		<remark>
//		<function>
//		<block_time>
//		<flt_duty_period>
//		<duty_period>
//	</event>
//</planning>
