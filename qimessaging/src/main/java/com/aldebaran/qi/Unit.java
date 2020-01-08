package com.aldebaran.qi;

/**
 * Type that conveys no information.
 *
 * It can be used in place of `Void`, with the benefit of being instantiable.
 *
 * Theoretically, it corresponds to the terminal object in the category of
 * sets and functions, which is the set with a single element.
 *
 * See <https://en.wikipedia.org/wiki/Unit_type>
 */
final class Unit {
    // Unique value of `Unit`.
    private static final Unit UNIT = new Unit();

    private Unit(){}

    public static Unit value() {
        return UNIT;
    }
}
