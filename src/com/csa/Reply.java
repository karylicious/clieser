/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csa;

import java.util.ArrayList;
import javax.xml.bind.annotation.*;

/**
 *
 * @author Carla-PC
 */

@XmlRootElement
public class Reply {
    private ArrayList<String> responseList;
    private ArrayList<TestResult> testResultList;
    
    Reply(){}
    
    Reply (ArrayList<String>  _responseList, ArrayList<TestResult> _testResultList){
        responseList = _responseList;
        testResultList = _testResultList;
    }
    @XmlElement
    public ArrayList<String> getResponseList() {return responseList;}
    
    @XmlElement
    public ArrayList<TestResult> getTestResultList() {return testResultList;}
}