package com.volchekD.SVN_Explorer;

import java.util.Stack;

public class URLStack<Type> extends Stack<Type> {
    public String toString(){
        String s="";
        for(int i=0; i <size(); i++)
            s+=elementAt(i);
        return s;

    }
}
