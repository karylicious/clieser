/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csa;

import javax.xml.bind.annotation.*;
/**
 *
 * @author Carla-PC
 */
@XmlRootElement
public class TestResult {
    private String title;
    private boolean result;
    
    TestResult(){}
    
    TestResult(String _title, boolean _result){
        title = _title;
        result = _result;
    }
    
    @XmlElement
    public String getTitle() {return title;}
    
    @XmlElement
    public boolean getResult() {return result;}
}