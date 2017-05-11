package com.aldebaran.qi.serialization;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import com.aldebaran.qi.QiField;

/**
 * Class and type information for field annotated with {@link QiField}
 *
 */
class QiFieldInformation implements Comparable<QiFieldInformation> {
    /**
     * Create information for given field
     *
     * @param field
     *            Field to get information from
     * @return Created information OR {@code null} if given field not annotated
     *         by {@link QiField}
     */
    static QiFieldInformation createInformation(final Field field) {
        final QiField qiField = field.getAnnotation(QiField.class);

        if (qiField == null) {
            return null;
        }

        return new QiFieldInformation(field.getType(), field.getGenericType(), qiField.value());
    }

    /** Field class */
    public final Class<?> clazz;
    /** Field type */
    public final Type type;
    /** Index of field in structure */
    public final int index;

    /**
     * Create information
     *
     * @param clazz
     *            Field class
     * @param type
     *            Field type
     * @param index
     *            Index of field in structure
     */
    private QiFieldInformation(final Class<?> clazz, final Type type, final int index) {
        this.clazz = clazz;
        this.type = type;
        this.index = index;
    }

    /**
     * Indicates if given object equals to this
     *
     * @param object
     *            Tested object
     * @return {@code true} if given object equals to this
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (null == object) {
            return false;
        }

        if (!QiFieldInformation.class.equals(object.getClass())) {
            return false;
        }

        final QiFieldInformation qiFieldInformation = (QiFieldInformation) object;
        return this.index == qiFieldInformation.index;
    }

    /**
     * Hash code
     *
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return this.index;
    }

    /**
     * Compare this object to given one to know the order.<br>
     * It returns:
     * <ul>
     * <li><b>Strictly negative</b>: if this object before given one</li>
     * <li><b>Zero</b>: if this object as same place as given one</li>
     * <li><b>Strictly positive</b>: if this object after given one</li>
     * </ul>
     */
    @Override
    public int compareTo(final QiFieldInformation qiFieldInformation) {
        return this.index - qiFieldInformation.index;
    }
}
