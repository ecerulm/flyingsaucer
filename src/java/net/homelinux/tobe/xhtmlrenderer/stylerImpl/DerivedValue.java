/*
 *
 * DerivedValue.java
 * Copyright (c) 2004 Torbj�rn Gannholm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package net.homelinux.tobe.xhtmlrenderer.stylerImpl;

import java.awt.Color;
import java.util.logging.*;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;
import org.w3c.dom.css.Counter;
import org.w3c.dom.css.Rect;

import com.pdoubleya.xhtmlrenderer.css.constants.CSSName;
import com.pdoubleya.xhtmlrenderer.css.constants.ValueConstants;
import com.pdoubleya.xhtmlrenderer.css.util.ConversionUtil;
import com.pdoubleya.xhtmlrenderer.util.LoggerUtil;


/**
 * A primitive value assigned to an XRProperty. XRValue allows for easy type
 * conversions, relative value derivation, etc. The class is intended to "wrap"
 * a CSSValue from a SAC CSS parser. Note that not all type conversions make
 * sense, and that some won't make sense until relative values are resolved. You
 * should check with the cssSACPrimitiveValueType() to see if the value
 * conversion you are requesting is rational. 
 *
 * @author    Patrick Wright
 *
 */
public class DerivedValue {
    /** Constant for CSS2 value of "important" */
    String IMPORTANT = "important";

    /** Constant for CSS2 value of "inherit" */
    String INHERIT = "inherit";

    /** Logger instance used for debug messages from this class. */
    private final static Logger sDbgLogger = LoggerUtil.getDebugLogger( DerivedValue.class );

    // ASK: need to clarify if this class is for both List and Primitives, or just primitives...
    
    /** The DOM CSSValue we are given from the Parse */
    private CSSValue _domCSSValue;
    
    private CalculatedStyle _inheritedStyle;
    
    /** HACK: if the DOM value was relative, and we convert to absolute, the new type of our value, after conversion; we have to store this separately for now because CSSValue has no API for changing type at runtime. */
    private short _newPrimitiveValueType;

    /** CLEANUP: is this needed (PWW 13/08/04) */
    private float _asFloat;

    /** CLEANUP: is this needed (PWW 13/08/04) */
    private boolean _requiresComputation;

    /** String array, if there is one to split from value */
    private String[] _stringAsArray;

    /**
     * Constructor for the XRValueImpl object
     *
     * @param domCSSValue  PARAM
     * @param domPriority  PARAM
     */
    /*public DerivedValue(CSSValue domCSSValue) {
        _domCSSValue = domCSSValue;
        //_domValueTextClean = getCssTextClean(domCSSValue);
        _newPrimitiveValueType = -1;
        _requiresComputation = ! ValueConstants.isAbsoluteUnit(domCSSValue);
        
        if ( ValueConstants.isNumber(cssSACPrimitiveValueType()) ) {
            if ( shouldConvertToPixels() ) {
                _asFloat = convertValueToPixels();
            } else {
                _asFloat = new Float( getCssTextClean(domCSSValue) ).floatValue();
            }
        }
    }*/

    /**
     * Constructor for the XRValueImpl object
     *
     * @param domCSSValue  PARAM
     * @param inheritedStyle  PARAM
     */
    public DerivedValue(CSSValue domCSSValue, CalculatedStyle inheritedStyle) {
        _domCSSValue = domCSSValue;
        //_domValueTextClean = getCssTextClean(domCSSValue);
        _newPrimitiveValueType = -1;
        _requiresComputation = ! ValueConstants.isAbsoluteUnit(domCSSValue);
        
        _inheritedStyle = inheritedStyle;
        
        if ( ValueConstants.isNumber(cssSACPrimitiveValueType()) ) {
            if ( shouldConvertToPixels() ) {
                _asFloat = convertValueToPixels();
            } else {
                _asFloat = new Float( getCssTextClean(domCSSValue) ).floatValue();
            }
        }
    }


    /**
     * Deep copy operation. However, any contained SAC instances are not
     * deep-copied.
     *
     * @return   Returns
     */
    //What is this used for?
    public DerivedValue copyOf() {
        DerivedValue nv = new DerivedValue( _domCSSValue, _inheritedStyle );
        //nv._newPrimitiveValueType = this._newPrimitiveValueType;
        return nv;
    }


    /**
     * The value as a float; returns Float.MIN_VALUE (as float) if there is an
     * error.
     *
     * @return   Returns
     */
    public float asFloat() {
        float f = new Float( Float.MIN_VALUE ).floatValue();
        try {
            f = _asFloat;
        } catch ( Exception ex ) {
            System.err.println( "Value '" + getCssTextClean(_domCSSValue) + "' is not a valid float." );
        }
        return f;
    }


    /**
     * value as a string...same as getStringValue() but kept for parallel with
     * other as <type>... methods
     *
     * @return   Returns
     */
    public String asString() {
        return getStringValue();
    }


    /**
     * Returns the value as assigned, split into a string array on comma.
     *
     * @return   Returns
     */
    public String[] asStringArray() {
        if ( _stringAsArray == null ) {
            String str = getStringValue();
            _stringAsArray = ( str == null ? new String[0] : str.split( "," ));
        }
        return _stringAsArray;
    }


    /**
     * The value as a CSSValue; changes to the CSSValue are not tracked. Any
     * changes to the properties should be made through the XRProperty and
     * XRValue classes.
     *
     * @return   Returns
     */
    public CSSValue cssValue() {
        return _domCSSValue;
    }


    /**
     * See interface.
     *
     * @return   See desc.
     */
    public boolean forcedInherit() {
        return _domCSSValue.getCssText().indexOf( INHERIT ) >= 0;
    }


    /**
     * A text representation of the value, for dumping
     *
     * @return   Returns
     */
    /*public String toString() {
        return getCssText() + " (" + ValueConstants.cssType(_domCSSValue.getCssValueType(), cssSACPrimitiveValueType()) + "--" + ValueConstants.getCssValueTypeDesc(cssValue()) + ")\n" +
                "   " + ( forcedInherit() ? "" : "not " ) + "inherited";
    }*/


    /**
     * Computes a relative unit (e.g. percentage) as an absolute value, using
     * the property's parentStyle
     *
     * @param parentStyle
     * @param propName      The name of the property to which this value is
     *      assigned; given because some relative values differ for font-size,
     *      etc.
     */
    public void computeRelativeUnit( CalculatedStyle parentStyle, String propName ) {
        if ( ValueConstants.isAbsoluteUnit(cssValue()) ) {
            sDbgLogger.info( "Was asked to convert a relative value, but value is absolute. Call isAbsolute() first." );
            return;
        }

        float relVal = new Float( getCssTextClean(_domCSSValue) ).floatValue();
        float absVal = 0F;
        String newTypeSuffix = "px";

        switch ( cssSACPrimitiveValueType() ) {
            case CSSPrimitiveValue.CSS_EMS:
                // EM is equal to font-size of element on which it is used
                // The exception is when �em� occurs in the value of
                // the �font-size� property itself, in which case it refers
                // to the font size of the parent element (spec: 4.3.2)
                absVal = relVal * deriveFontSize( parentStyle, propName );
                _newPrimitiveValueType = CSSPrimitiveValue.CSS_PX;

                break;
            case CSSPrimitiveValue.CSS_EXS:
                // HACK: just to convert the value to something meaningful, using the height of the 'x' character
                // on the default system font.
                // To convert EMS to pixels, we need the height of the lowercase 'x' character in the current
                // element...
                float xHeight = parentStyle.propertyByName("font-size").computedValue().asFloat();

                absVal = relVal * xHeight;
                _newPrimitiveValueType = CSSPrimitiveValue.CSS_PX;
                break;
            case CSSPrimitiveValue.CSS_PX:
                // nothing to do
                absVal = relVal;
                break;
            case CSSPrimitiveValue.CSS_PERCENTAGE:
                // percentage depends on the property this value belongs to
                float base = 1.0F;
                if ( propName.equals( CSSName.BOTTOM ) ) {
                    // TODO: need height of containing block
                    System.err.println( "Value not available: property '" + CSSName.BOTTOM + "' as % requires height of containing block." );
                } else if ( propName.equals( CSSName.TOP ) ) {
                    // TODO: need height of containing block
                    System.err.println( "Value not available: property '" + CSSName.TOP + "' as % requires height of containing block." );
                } else if ( propName.equals( CSSName.LEFT ) ) {
                    // TODO: need height of containing block
                    System.err.println( "Value not available: property '" + CSSName.LEFT + "' as % requires width of containing block." );
                } else if ( propName.equals( CSSName.RIGHT ) ) {
                    // TODO: need height of containing block
                    System.err.println( "Value not available: property '" + CSSName.RIGHT + "' as % requires width of containing block." );
                } else if ( propName.equals( CSSName.HEIGHT ) ) {
                    // TODO: need height of containing block
                    System.err.println( "Value not available: property '" + CSSName.HEIGHT + "' as % requires height of containing block." );
                } else if ( propName.equals( CSSName.MAX_HEIGHT ) ) {
                    // TODO: need height of containing block
                    System.err.println( "Value not available: property '" + CSSName.MAX_HEIGHT + "' as % requires height of containing block." );
                } else if ( propName.equals( CSSName.MIN_HEIGHT ) ) {
                    // TODO: need height of containing block
                    System.err.println( "Value not available: property '" + CSSName.MIN_HEIGHT + "' as % requires height of containing block." );
                } else if ( propName.equals( CSSName.MAX_WIDTH ) ) {
                    // TODO: need height of containing block
                    System.err.println( "Value not available: property '" + CSSName.MAX_WIDTH + "' as % requires width of containing block." );
                } else if ( propName.equals( CSSName.MIN_WIDTH ) ) {
                    // TODO: need height of containing block
                    System.err.println( "Value not available: property '" + CSSName.MIN_WIDTH + "' as % requires width of containing block." );
                } else if ( propName.equals( CSSName.TEXT_INDENT ) ) {
                    // TODO: need height of containing block
                    System.err.println( "Value not available: property '" + CSSName.TEXT_INDENT + "' as % requires width of containing block." );
                } else if ( propName.equals( CSSName.VERTICAL_ALIGN ) ) {
                    base = parentStyle.propertyByName( CSSName.LINE_HEIGHT ).computedValue().asFloat();

                } else if ( propName.equals( CSSName.FONT_SIZE ) ) {
                    // same as with EM
                    base = deriveFontSize( parentStyle, propName );
                    _newPrimitiveValueType = CSSPrimitiveValue.CSS_PT;
                    newTypeSuffix = "pt";
                    _newPrimitiveValueType = CSSPrimitiveValue.CSS_PX;
                }
                absVal = ( relVal / 100 ) * base;
                // CLEAN System.out.println("New calculated abs val: " + absVal);
                break;
            default:
                // nothing to do, we only convert those listed above
                System.err.println( "Asked to convert value from relative to absolute, don't recognize the datatype " + toString() );
        }
        assert( new Float( absVal ).intValue() > 0 );
        sDbgLogger.finer( "Converted '" + propName + "' relative value of " + relVal + " (" + _domCSSValue.getCssText() + ") to absolute value of " + absVal );
        
        // round down
        double d = Math.floor((double)absVal);
        _asFloat = new Float(d).floatValue();
        
        // note--this is an important step, because if the value is ever
        // inherited, the child will inherit a copy, which will at some
        // point parse the text looking for the type code--so need the suffix
        setCssText("" + _asFloat + newTypeSuffix);
        _requiresComputation = false;
    }


    /**
     * HACK: this only works if the value is actually a primitve
     *
     * @return   The rGBColorValue value
     */
    public Color asColor() {
        assert( getCssValueType(_domCSSValue) == CSSValue.CSS_PRIMITIVE_VALUE );
        String str = _domCSSValue.getCssText();
        if ( "transparent".equals(str)) 
            return new Color(0,0,0,0);
        else
            return ConversionUtil.rgbToColor( ( (CSSPrimitiveValue)_domCSSValue ).getRGBColorValue() );
    }


    /**
     * Returns true if this is a relative unit (e.g. percentage) whose value has
     * been computed as an absolute computed value, or if by chance this is an
     * absolute unit.
     *
     * @return   The relativeUnitComputed value
     */
    public boolean requiresComputation() {
        return _requiresComputation;
    }


    /**
     * See interface.
     *
     * @param index  The new stringValue value
     * @param s      The new stringValue value
     */
    public void setStringValue( short index, String s ) {
        assert( getCssValueType(_domCSSValue) == CSSValue.CSS_PRIMITIVE_VALUE );
        ( (CSSPrimitiveValue)_domCSSValue ).setStringValue( index, s );
    }


    /**
     * See interface.
     *
     * @param unitType  The new floatValue value
     * @param val       The new floatValue value
     */
    public void setFloatValue( short unitType, float val ) {
        assert( getCssValueType(_domCSSValue) == CSSValue.CSS_PRIMITIVE_VALUE );
        ( (CSSPrimitiveValue)_domCSSValue ).setFloatValue( unitType, val );
    }


    /**
     * Sets the cssText attribute of the XRValueImpl object
     *
     * @param str               The new cssText value
     * @exception DOMException  Throws
     */
    public void setCssText( String str )
        throws DOMException {
        _domCSSValue.setCssText( str );
    }


    /**
     * See interface.
     *
     * @return   Returns
     */
    public short getPrimitiveType() {
        assert( getCssValueType(_domCSSValue) == CSSValue.CSS_PRIMITIVE_VALUE );
        return ( (CSSPrimitiveValue)_domCSSValue ).getPrimitiveType();
    }


    /**
     * See interface.
     *
     * @return   Returns
     */
    public String getStringValue() {
        assert( getCssValueType(_domCSSValue) == CSSValue.CSS_PRIMITIVE_VALUE );
        return ( (CSSPrimitiveValue)_domCSSValue ).getStringValue();
    }


    /**
     * See interface.
     *
     * @param unitType  PARAM
     * @return          Returns
     */
    public float getFloatValue( short unitType ) {
        assert( getCssValueType(_domCSSValue) == CSSValue.CSS_PRIMITIVE_VALUE );
        return ( (CSSPrimitiveValue)_domCSSValue ).getFloatValue( unitType );
    }


    /**
     * See interface.
     *
     * @return   Returns
     */
    public Counter getCounterValue() {
        assert( getCssValueType(_domCSSValue) == CSSValue.CSS_PRIMITIVE_VALUE );
        return ( (CSSPrimitiveValue)_domCSSValue ).getCounterValue();
    }


    /**
     * See interface.
     *
     * @return   Returns
     */
    public Rect getRectValue() {
        assert( getCssValueType(_domCSSValue) == CSSValue.CSS_PRIMITIVE_VALUE );
        return ( (CSSPrimitiveValue)_domCSSValue ).getRectValue();
    }


    /**
     * Gets the primitiveType attribute of the XRValueImpl object
     *
     * @return   The primitiveType value
     */
    public boolean isPrimitiveType() {
        return getCssValueType(_domCSSValue) == CSSValue.CSS_PRIMITIVE_VALUE;
    }


    /**
     * Gets the valueList attribute of the XRValueImpl object
     *
     * @return   The valueList value
     */
    public boolean isValueList() {
        return getCssValueType(_domCSSValue) == CSSValue.CSS_VALUE_LIST;
    }


    /**
     * Gets the cssText attribute of the XRValueImpl object
     *
     * @return   The cssText value
     */
    /*public String getCssText() {
        return _domCSSValue.getCssText();
    }*/


    /**
     * Gets the cssValueType attribute of the XRValueImpl object
     *
     * @return   The cssValueType value
     */
    public static short getCssValueType(CSSValue cssval) {
        return cssval.getCssValueType();
    }


    // the CSSValue type if we are wrapping a CSSValue, type
    // CSSValue.CSS_UNKNOWN if we are not wrapping a primitive; best to
    // check if we are wrapping a primitive first
    /**
     * See interface.
     *
     * @return   Returns
     */
    private short cssSACPrimitiveValueType() {
        assert( getCssValueType(_domCSSValue) == CSSValue.CSS_PRIMITIVE_VALUE );
        
        if ( _newPrimitiveValueType >= 0 ) 
            return _newPrimitiveValueType;
        else 
            return ( (CSSPrimitiveValue)_domCSSValue ).getPrimitiveType();
    }


    /**
     * See interface.
     *
     * @param ownerElement  PARAM
     * @param propName      PARAM
     * @return              Returns
     */
    private float deriveFontSize( CalculatedStyle parentStyle, String propName ) {
        float fontSize = 0F;
        if ( propName.equals( CSSName.FONT_SIZE ) && parentStyle != null ) {
            //TODO: this is probably wrong
            fontSize = parentStyle.propertyByName( CSSName.FONT_SIZE ).computedValue().asFloat();
        } else {
System.err.println("ERROR: Trying to derive font size wrongly in "+this.getClass().getName());
        }
        return fontSize;
    }


    /**
     * See interface.
     *
     * @return   Returns
     */
    private float convertValueToPixels() {
        assert( shouldConvertToPixels() );

        float pixelVal = new Float( Float.MIN_VALUE ).floatValue();

        float startVal = new Float( getCssTextClean(_domCSSValue) ).floatValue();

        final float MM_PER_PX = 0.28F;
        final int MM_PER_CM = 10;
        final float CM_PER_IN = 2.54F;
        final float PT_PER_IN = 72;
        final float PC_PER_PT = 12;

        float cm;

        float mm;

        float in;

        float pt;

        float pc = 0.0F;

        switch ( cssSACPrimitiveValueType() ) {
            case CSSPrimitiveValue.CSS_EMS:
                // TODO
                pixelVal = startVal;
                break;
            case CSSPrimitiveValue.CSS_EXS:
                // TODO
                pixelVal = startVal;
                break;
            case CSSPrimitiveValue.CSS_PX:
                // nothing to do
                pixelVal = startVal;
                break;
            case CSSPrimitiveValue.CSS_PERCENTAGE:
                // TODO
                break;
            // length
            case CSSPrimitiveValue.CSS_IN:
                cm = startVal * CM_PER_IN;
                mm = cm * MM_PER_CM;
                pixelVal = mm / MM_PER_PX;
                break;
            case CSSPrimitiveValue.CSS_CM:
                cm = startVal;
                mm = cm * MM_PER_CM;
                pixelVal = mm / MM_PER_PX;
                break;
            case CSSPrimitiveValue.CSS_MM:
                mm = startVal;
                pixelVal = mm / MM_PER_PX;
                break;
            case CSSPrimitiveValue.CSS_PT:
                pt = startVal;
                in = pt * PT_PER_IN;
                cm = in * CM_PER_IN;
                mm = cm * MM_PER_CM;
                pixelVal = mm / MM_PER_PX;
                break;
            case CSSPrimitiveValue.CSS_PC:
                pc = startVal;
                pt = pc * PC_PER_PT;
                in = pt * PT_PER_IN;
                cm = in * CM_PER_IN;
                mm = cm * MM_PER_CM;
                pixelVal = mm / MM_PER_PX;
                break;
        }
        return pixelVal;
    }


    /**
     * Gets the length attribute of the XRValueImpl object
     *
     * @return   The length value
     */
    private boolean shouldConvertToPixels() {
        return ValueConstants.isNumber(cssSACPrimitiveValueType()) && cssSACPrimitiveValueType() != CSSPrimitiveValue.CSS_PT;
    }


    /**
     * Gets the cssText attribute of the XRValueImpl object
     *
     * @return   The cssText value
     */
    private static String getCssTextClean(CSSValue cssval) {
        String text = cssval.getCssText().trim();
        // TODO: use regex to pull out all possible endings
        if ( text.endsWith( "px" ) || text.endsWith( "pt" ) || text.endsWith( "em" ) ) {
            text = text.substring( 0, text.length() - 2 ).trim();
        } else if ( text.endsWith( "%" ) ) {
            text = text.substring( 0, text.length() - 1 ).trim();
        }
        return text;
    }
}// end class
