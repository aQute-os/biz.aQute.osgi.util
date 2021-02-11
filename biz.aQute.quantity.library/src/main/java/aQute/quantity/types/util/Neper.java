package aQute.quantity.types.util;

import aQute.quantity.base.util.Logarithmic;
import aQute.quantity.base.util.Quantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnnamedQuantity;

public class Neper extends Quantity<Neper> {
	
	Neper(double value) {
		super(value);
	}

	private static final long serialVersionUID = 1L;

	@Override
	protected Neper same(double value) {
		return Neper.from(value);
	}

	public static Neper from(double value) {
		return new Neper(value);
	}

	@Override
	public Unit getUnit() {
		return Unit.DIMENSIONLESS;
	}
	
	@Override
	public Neper mul( double value) {
		return Neper.from( this.value + Math.log(value) );
	}
	
	public Neper mul( Neper neper) {
		return Neper.from( this.value + neper.value );
	}
	
	public Neper mul( Logarithmic<?> src) throws Exception {
		return Neper.from( this.value + src.toNeper() );
	}

	public Neper div( Logarithmic<?> src) throws Exception {
		return Neper.from( this.value - src.toNeper() );
	}

	
	@Override
	public Neper div( double value) {
		return Neper.from( this.value - Math.log(value) );
	}
	
	public Neper div( Neper neper) {
		return Neper.from( this.value - neper.value );
	}
	
	@Override
	@Deprecated
	public Neper add(Neper mul) {
		return super.add(mul);
	}
	
	@Override
	@Deprecated
	public Neper sub(Neper mul) {
		return super.sub(mul);
	}
	

	@Deprecated
	public Quantity<?> mul( Quantity<?> src) throws Exception {
		return UnnamedQuantity.from( value * src.value, getUnit().add(src.getUnit()));
	}

	@Deprecated
	public Quantity<?> div( Quantity<?> src) throws Exception {
		return UnnamedQuantity.from( value / src.value, getUnit().add(src.getUnit()));
	}

}
