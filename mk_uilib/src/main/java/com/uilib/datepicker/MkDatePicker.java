package com.uilib.datepicker;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import com.uilib.R;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Mikiller on 2018/4/2.
 */

public class MkDatePicker extends LinearLayout {
    private NumberPicker yearPicker, monthPicker, dayPicker;
    private int year, month, day;
    private Calendar calendar;

    public MkDatePicker(Context context) {
        this(context, null, 0);
    }

    public MkDatePicker(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MkDatePicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }

    private void initView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.layout_date_picker, this);
        yearPicker = (NumberPicker) findViewById(R.id.yearPicker);
        monthPicker = (NumberPicker) findViewById(R.id.monthPicker);
        dayPicker = (NumberPicker) findViewById(R.id.dayPicker);
        calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        day = calendar.get(Calendar.DAY_OF_MONTH);
        yearPicker.setMinValue(1970);
        yearPicker.setMaxValue(year);
        yearPicker.setWrapSelectorWheel(false);
        yearPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateDatePicker(newVal, month);
            }
        });
        yearPicker.setValue(year);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateDatePicker(year, newVal);
            }
        });
        monthPicker.setValue(month);

        dayPicker.setMinValue(1);
        updateDatePicker(year, month);
        dayPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                calendar.set(Calendar.DATE, day = newVal);
            }
        });
        dayPicker.setValue(day);

        TypedValue tv = new TypedValue();
        setNumberPickerDividerColor(yearPicker, getContext().getResources().getColor(R.color.colorPrimary));
        setNumberPickerDividerColor(monthPicker, getContext().getResources().getColor(R.color.colorPrimary));
        setNumberPickerDividerColor(dayPicker, getContext().getResources().getColor(R.color.colorPrimary));
    }

    private void updateDatePicker(int year, int month){
        if(year != this.year)
            calendar.set(Calendar.YEAR, this.year = year);
        if(month != this.month)
            calendar.set(Calendar.MONTH, (this.month = month) - 1);
        calendar.set(Calendar.DATE,1);
        calendar.roll(Calendar.DATE, -1);
        dayPicker.setDisplayedValues(null);
        dayPicker.setMaxValue(calendar.get(Calendar.DATE));
    }

    private static void setNumberPickerDividerColor(NumberPicker numberPicker, int color) {
        Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (Field SelectionDividerField : pickerFields) {
            if (SelectionDividerField.getName().equals("mSelectionDivider")) {
                SelectionDividerField.setAccessible(true);
                try {
                    SelectionDividerField.set(numberPicker, new ColorDrawable(color));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }


    public String getDateStr(String format){
        return String.format(format, year, month, day);
    }

    public long getDateStamp(){
        calendar.set(Calendar.DATE, day);
        return calendar.getTimeInMillis();
    }


}
