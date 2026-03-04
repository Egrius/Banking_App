package org.example.mapper;

public interface BaseMapper <F, T> {
    T map(F object);

    default T map(F fromObject, T toObject) {return toObject;}

}