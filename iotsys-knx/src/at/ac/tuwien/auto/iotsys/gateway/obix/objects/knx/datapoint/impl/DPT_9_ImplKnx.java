package at.ac.tuwien.auto.iotsys.gateway.obix.objects.knx.datapoint.impl;

import java.util.logging.Logger;

import obix.Obj;
import at.ac.tuwien.auto.calimero.GroupAddress;
import at.ac.tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat;
import at.ac.tuwien.auto.calimero.exception.KNXException;
import at.ac.tuwien.auto.iotsys.commons.obix.objects.general.datapoint.impl.DPT_9_Impl;
import at.ac.tuwien.auto.iotsys.gateway.connectors.knx.KNXConnector;
import at.ac.tuwien.auto.iotsys.gateway.connectors.knx.KNXWatchDog;

public class DPT_9_ImplKnx extends DPT_9_Impl
{
	private static final Logger log = Logger.getLogger(DPT_9_ImplKnx.class.getName());

	private GroupAddress groupAddress;
	private KNXConnector connector;
	private boolean readable;
	private boolean writable;

	// if more group addresses are needed just add more constructor parameters.
	public DPT_9_ImplKnx(KNXConnector connector, GroupAddress groupAddress, String name, String displayName, String display, boolean writable, boolean readable)
	{
		super(name, displayName, display, writable);

		this.groupAddress = groupAddress;
		this.connector = connector;
		this.writable = writable;
		this.readable = readable;

		// if it is not possible to read from the group address --> create a watchdog that monitors the communication
		if (!this.readable)
			this.createWatchDog();
	}
	
	public DPT_9_ImplKnx(KNXConnector connector, DataPointInit dataPointInit)
	{
		this(connector, dataPointInit.getGroupAddress(), dataPointInit.getName(), dataPointInit.getDisplayName(), dataPointInit.getDisplay(), dataPointInit.isWritable(), dataPointInit.isReadable());
	}

	public void createWatchDog()
	{
		connector.addWatchDog(groupAddress, new KNXWatchDog()
		{
			@Override
			public void notifyWatchDog(byte[] apdu)
			{
				try
				{
					DPTXlator2ByteFloat x = new DPTXlator2ByteFloat(DPTXlator2ByteFloat.DPT_ELECTRICAL_CURRENT);

					x.setData(apdu, 0);

					log.fine("2Byte Float for " + DPT_9_ImplKnx.this.getHref() + " now " + x.getValueFloat(1));

					value.set(x.getValueFloat(1));
				}
				catch (KNXException e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void refreshObject()
	{
		// here we need to read from the bus, only if the read flag is set at the data point
		if (this.readable)
		{
			float value = connector.readFloat(groupAddress);
			this.value().set(value);
		}
	}

	@Override
	public void writeObject(Obj obj)
	{
		if (this.writable)
		{
			// always pass the writeObject call to the super method (triggers, oBIX related internal services like watches, alarms, ...)
			// also the internal instance variables get updated
			super.writeObject(obj);

			// now write this.value to the KNX bus
			connector.write(groupAddress, this.value().get());
		}
	}
}
