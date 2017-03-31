package com.bridge3;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.ss.usermodel.Cell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Jonathan Beaulieu on 1/25/15.
 */
public class LogData implements Parcelable{
    List<List<String>> data = new ArrayList<List<String>>();
    Integer blowsPerAve = 10;
    Integer blowCount = 1;
    Integer lastMark = 1;
    Integer markNumber = 1;
    Double unitFactor = 1.0;

    public LogData() {
        List<String> header = new ArrayList();
        header.add("Blow #");
        header.add("Last");
        if(blowsPerAve == 9999){
            header.add("Mark/Ave");
        }else {
            header.add(blowsPerAve + "/Ave");
        }
        header.add("Depth");
        data.add(header);
    }

    public void reset() {
        data = new ArrayList<List<String>>();
        List<String> header = new ArrayList();
        header.add("Blow #");
        header.add("Last");
        if (blowsPerAve==9999) {
            header.add("Mark/Ave");
        } else {
            header.add(blowsPerAve + "/Ave");
        }
        header.add("Depth");
        data.add(header);
        blowCount = 1;
        lastMark = 1;
        markNumber = 1;
    }

    public void setUnitFactor(Double unitFactor) {
        this.unitFactor = unitFactor;
    }

    public void setBlowsPerAve(Integer blowsPerAve) {
        this.blowsPerAve = blowsPerAve;
        this.data.get(0).set(2, blowsPerAve + "/Ave");
    }

    public Integer getBlowsPerAve() {
        return blowsPerAve;
    }

    public Integer size() {
        return this.data.size();
    }

    public void add(String last) {
        List<String> row = new ArrayList();
        row.add("" + this.blowCount);
        row.add(String.valueOf(Double.parseDouble(last)));
        row.add("----");
        row.add("");
        data.add(row);
        this.blowCount++;
    }

    public void mark() {
        if (blowCount.equals(lastMark)) {
            return;
        }
        this.data.get(size()-1).set(3, markNumber + "=" + (blowCount - lastMark));
        markNumber++;
        lastMark = blowCount;
    }

    public Double getAve(int index) {
        int itemsPerAve = this.getBlowsPerAve();
        if (itemsPerAve > index) {
            itemsPerAve = index;
        }

        double sum = 0.0;

        int i;

        for (i=index; i > index - itemsPerAve; i--) {
            sum += new Double(this.data.get(i).get(1)) * unitFactor; //ADDED UNITFACTOR TO VOID ADD() SO WE DON'T HAVE TO WORRY ABOUT THIS
//            sum += new Double(this.data.get(i).get(1));
        }

//        System.out.println("Ave of: " + itemsPerAve + " i: " + i);

        return sum / itemsPerAve;
    }

    public List<String> getRow(int row) {
        if (row==0) {
            List<String> header = new ArrayList();
            header.add("Blow #");
            header.add("Last");
            if (blowsPerAve==9999) {
                header.add("Mark/Ave");
            } else {
                header.add(blowsPerAve + "/Ave");
            }
            header.add("Depth");
            return header;
//            return this.data.get(0);
        }
        List<String> r = new ArrayList<String>(4);
        r.add(this.data.get(row).get(0));
        r.add(String.format("%.1f", new Double(this.data.get(row).get(1))*unitFactor));
//        r.add(String.format("%.2f", new Double(this.data.get(row).get(1))));
        if (blowsPerAve == 9999) {
            if (this.data.get(row).get(3) != null && !this.data.get(row).get(3).equals("")) {
                r.add(String.format("%.1f", getStrikesPerMark(row)));
            } else {
                r.add("----");
            }
        } else {
            r.add(String.format("%.1f", this.getAve(row)));
        }
        r.add(this.data.get(row).get(3));
        return r;
    }

    public List<String> getNonFormatedRow(int row) {
        if (row==0) {
            return this.data.get(0);
        }
        List<String> r = this.data.get(row);
        r.set(2, ""+this.getAve(row));
        return r;
    }

    public double getBlowsPerMinute(){
        double time = 0;
        double blowsMin=0;
        time = getAve(size()-1) / unitFactor;
        time += .3;
        time /= 4;
        time = Math.sqrt(time);
        blowsMin = 60/time;
        return blowsMin;
    }
    public double getBlowsPerUnit(){
//        double blowsPerUnit = 0;
//        blowsPerUnit = 1/getLastBlow();
        //This is just gonna return the markBlow total now
        //MARK NUMBER IS THE NEXT IN THE SEQUENCE SO -1 FOR CURRENT
        return blowCount - lastMark;
    }
    public double getLastBlow(){
        if (size() <= 1) {
            return 0;
        }
        return Double.parseDouble(data.get(size()-1).get(1)) * unitFactor;
//        return Double.parseDouble(data.get(size()-1).get(1));
    }
    public double getLastUnitAve(){
        if(blowsPerAve == 9999){
            //GONNA MAKE A LISTENER FOR THE HITTING OF MARK BLOW IN THE MAIN TO MAKE THIS WORK BETTER
            //THIS IS GONNA BE FOR THE MARK BLOW AVERAGE FT NONSENSE
            if (markNumber != 1) {
                return getStrikesPerMark(lastMark-1);
            } else {
                return -1;
            }
        }
        return getAve(size() - 1);
    }

    //pretty much gonna make a find row in here just so I can get the previous ros before it that I need.
    public double getStrikesPerMark(int index){
        System.out.println("getStrikesPerkMark");

        List<String> row = data.get(index);
        double sum = 0;
        int diff = 0;
        try {
            diff = Integer.parseInt(row.get(3).substring(row.get(3).indexOf("=") + 1, row.get(3).length()));
        } catch (NumberFormatException e) {
            return -1;
        }
        for (int i=index; i > index-diff; i--) {
            sum += Double.parseDouble(data.get(i).get(1));
        }
        return (sum*unitFactor) / diff;


//        return total/diff;
    }

    //excel methods
    public Uri createSheet(Context context) {
        File file = null;
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Log Data");

        HSSFRow row = sheet.createRow(0);
        for (int c = 0; c < 4; c++) {
            Cell cell = row.createCell(c);
            cell.setCellValue(this.getRow(0).get(c));
        }

        for (int i = 1; i < this.size(); i++) {
            row = sheet.createRow(i);
            for (int c = 0; c < 4; c++) {
                Cell cell = row.createCell(c);
                String s = this.getRow(i).get(c);
                switch (c) {
                    case 0:
                        cell.setCellValue(new Integer(s));
                        break;
                    case 1:
                        cell.setCellValue(new Double(s));
                        break;
                    case 2:
                        try {
                            cell.setCellValue(new Double(s));
                        } catch (NumberFormatException e) {
                            cell.setCellValue("");
                        }
                        break;
                    case 3:
                        cell.setCellValue(s);
                        break;
                    default:
                        System.err.println("This is a major problem!!! check LogData.java:public Uri createSheet(Context context)");
                }
            }
        }

        try {
            File outputDir = context.getExternalCacheDir();
            file = File.createTempFile("LogData", ".xls", outputDir);
            if (file != null) {
                FileOutputStream os = new FileOutputStream(file);
                workbook.write(os);
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(file);
    }

    //Parcelable methods
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.blowsPerAve);
        out.writeInt(this.blowCount);
        out.writeInt(this.lastMark);
        out.writeInt(this.markNumber);
        out.writeDouble(this.unitFactor);
        out.writeList(this.data);
    }

    public static final Parcelable.Creator<LogData> CREATOR = new Parcelable.Creator<LogData>() {
        public LogData createFromParcel(Parcel in) {
            return new LogData(in);
        }

        public LogData[] newArray(int size) {
            return new LogData[size];
        }
    };


    private LogData(Parcel in) {
        this.blowsPerAve = in.readInt();
        this.blowCount = in.readInt();
        this.lastMark = in.readInt();
        this.markNumber = in.readInt();
        this.unitFactor = in.readDouble();
        in.readList(this.data, List.class.getClassLoader());
    }
}


//-(double)calcAvePer:(int)itemsPerAve
//{
//    int dataCount = data.count;
//    if (itemsPerAve > dataCount-1) {
//    itemsPerAve = dataCount-1;
//}
//
//    double sum = 0.0;
//
//    for (int i=dataCount-1; i > dataCount - (itemsPerAve+1); i--) {
//        sum += [[[data objectAtIndex:i] objectAtIndex:1] doubleValue];
//    }
//
//    return sum / itemsPerAve;
//}
