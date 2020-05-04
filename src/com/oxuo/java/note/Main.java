package com.oxuo.java.note;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class Main {

    public static void main(String[] args) {
        Vector<Integer> vec1 = new Vector<>();
        vec1.add(1);
        vec1.add(2);
        vec1.add(3);
        vec1.add(4);
        vec1.add(5);

        ArrayList<Integer> arr1 = new ArrayList<>();
        arr1.add(1);
        arr1.add(2);
        arr1.add(3);
        arr1.add(4);
        arr1.add(5);

        System.out.println("start test for subList of ArrayList");
//        testSubListForArrayList(arr1);
        System.out.println("-------- test end ---------");

        System.out.println("start test for subList of vector");
        testSubListForVector(vec1);
        System.out.println("-------- test end ---------");
    }

    private static void testSubListForArrayList(ArrayList<Integer> arr1) {
        List<Integer> subArr = arr1.subList(2,5);
        System.out.println("This is subList of ArrayList");
        subArr.forEach(item -> System.out.print(item + " "));
        System.out.println("");
        System.out.println("Starting non-structural change for sourceList ...");
        arr1.set(3,10);
        System.out.println("This is sourceList after non-structural change");
        print(arr1);
        System.out.println("This is subList after non-structural change");
        print(subArr);

        System.out.println("Starting non-structural change for subList ...");
        subArr.set(1,4);
        System.out.println("This is subList after non-structural change");
        print(arr1);
        System.out.println("This is subList after non-structural change");
        print(subArr);

        System.out.println("Starting structural change for subList ...");
        subArr.add(6);
        System.out.println("This is subList after structural change");
        print(arr1);
        System.out.println("This is subList after structural change");
        print(subArr);

        System.out.println("Starting structural change for sourceList ...");
        arr1.remove(5);
        System.out.println("This is subList after structural change");
        print(arr1);
        System.out.println("This is subList after structural change");
        print(subArr);
    }

    private static void testSubListForVector(Vector<Integer> vec) {
        List<Integer> subVec = vec.subList(2,5);
        System.out.println("This is subList of Vector");
        subVec.forEach(item -> System.out.print(item + " "));
        System.out.println("");
        System.out.println("Starting non-structural change for sourceList ...");
        vec.set(3,10);
        System.out.println("This is sourceList after non-structural change");
        print(vec);
        System.out.println("This is subList after non-structural change");
        print(subVec);

        System.out.println("Starting non-structural change for subList ...");
        subVec.set(1,4);
        System.out.println("This is subList after non-structural change");
        print(vec);
        System.out.println("This is subList after non-structural change");
        print(subVec);

        System.out.println("Starting structural change for subList ...");
        subVec.add(6);
        System.out.println("This is subList after structural change");
        print(vec);
        System.out.println("This is subList after structural change");
        print(subVec);

        System.out.println("Starting structural change for sourceList ...");
        vec.remove(5);
        System.out.println("This is subList after structural change");
        print(vec);
        System.out.println("This is subList after structural change");
        print(subVec);
    }

    private static void print(List list) {
        for (Object object : list) {
            System.out.print(object + " ");
        }
        System.out.println("");
    }
}
