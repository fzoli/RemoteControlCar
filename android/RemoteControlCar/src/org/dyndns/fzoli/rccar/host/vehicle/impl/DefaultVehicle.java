package org.dyndns.fzoli.rccar.host.vehicle.impl;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;

import org.dyndns.fzoli.rccar.host.ConnectionBinder;
import org.dyndns.fzoli.rccar.host.vehicle.AbstractVehicle;

/**
 * @author zoli
 */
public class DefaultVehicle extends AbstractVehicle {

	private PwmOutput pwm;
	private DigitalOutput outLeft, outRight, outFront, outBack;
	
	public DefaultVehicle(ConnectionBinder binder) {
		super(binder);
	}

	@Override
	public boolean isFullX() {
		return true;
	}

	@Override
	public boolean isFullY() {
		return true;
	}
	
	@Override
	protected void setup() throws ConnectionLostException, InterruptedException {
		super.setup();
		outFront = ioio_.openDigitalOutput(10, false);
		outBack = ioio_.openDigitalOutput(11, false);
		outLeft = ioio_.openDigitalOutput(12, false);
		outRight = ioio_.openDigitalOutput(13, false);
		pwm = ioio_.openPwmOutput(14, 1000);
	}
	
	/**
	 * Ez alapján arra jutottam, hogy ez a metódus, mint egy ciklus, állandóan ismétlődik, amíg van kapcsolat az IC-vel.
	 * A beépített LED, akkor világít, ha a digitális kimenetre logikai hamis van küldve, egyébként nem világít.
	 * Ezért, amikor megnyitom a digitális kimenetet, a kezdőérték true, hogy ne villágítson a led a loop metódus meghívása előtt.
	 * A többi kimenetnél már nincs ez a fordított logika, ezért a kezdőérték false!
	 */
	@Override
	public void loop() throws ConnectionLostException, InterruptedException {
		handle(getX(), outLeft, outRight);
		handle(getY(), outBack, outFront);
		pwm.setDutyCycle((float)(getY() / 100.0));
		Thread.sleep(20);
	}
	
	/**
	 * A jel alapján átváltja a két kimenetet úgy, hogy egy időben egyszerre a két kimenet soha nem aktív.
	 */
	private void handle(int sign, DigitalOutput outMinus, DigitalOutput outPlus) throws ConnectionLostException {
		if (sign == 0) {
			outMinus.write(false);
			outPlus.write(false);
		}
		else if (sign < 0) {
			outPlus.write(false);
			outMinus.write(true);
		}
		else {
			outMinus.write(false);
			outPlus.write(true);
		}
	}
	
}