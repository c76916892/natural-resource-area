/*
 *    natural-resource-area
 *    Copyright (C) 2025  ketikai
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pers.ketikai.minecraft.spigot.naturalresourcearea.exception;

public class NaturalResourceAreaException extends RuntimeException {
    private static final long serialVersionUID = 6436668834794874307L;

    public NaturalResourceAreaException() {}

    public NaturalResourceAreaException(String message) {
        super(message);
    }

    public NaturalResourceAreaException(String message, Throwable cause) {
        super(message, cause);
    }

    public NaturalResourceAreaException(Throwable cause) {
        super(cause);
    }

    protected NaturalResourceAreaException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
