import java.math.BigInteger;
import org.neo.smartcontract.framework.Helper;
import org.neo.smartcontract.framework.SmartContract;
import org.neo.smartcontract.framework.services.neo.Blockchain;
import org.neo.smartcontract.framework.services.neo.Runtime;
import org.neo.smartcontract.framework.services.neo.Storage;
import org.neo.smartcontract.framework.services.neo.Transaction;
import org.neo.smartcontract.framework.services.neo.TransactionOutput;
import org.neo.smartcontract.framework.services.neo.TriggerType;
import org.neo.smartcontract.framework.services.system.ExecutionEngine;

public class SampleEx2 extends SmartContract {
	private static final byte[] Other = {};
	private static final byte[] token_sales = {};
	private static BigInteger factor = BigInteger.valueOf(10000000);
	private static BigInteger ppre_ico_cap = BigInteger.valueOf(594800000);
	private static BigInteger pre_ico_cap = factor.multiply(ppre_ico_cap);
	private static BigInteger limit_time = BigInteger.valueOf(21600);
	private static BigInteger ico_start_time = BigInteger.valueOf(1513947600);
	private static BigInteger ico_end_time = BigInteger.valueOf(1516626000);
	private static BigInteger ico_duration = ico_end_time.subtract(ico_start_time);
	private static BigInteger bbasic_rate = BigInteger.valueOf(520);
	private static BigInteger basic_rate = bbasic_rate.multiply(factor);
	private static BigInteger ttotal_amount = BigInteger.valueOf(600000000);
	private static BigInteger total_amount = ttotal_amount.multiply(factor);
	private static long neo_decimals = 100000000;
	private static long limit_neo = 15 * neo_decimals;
	private static String contribute_prefix = "prefix";
	static byte[] neo_asset_id = new byte[] { (byte) 155, (byte) 124,
			(byte) 255, (byte) 218, (byte) 166, (byte) 116, (byte) 190,
			(byte) 174, (byte) 15, (byte) 147, (byte) 14, (byte) 190,
			(byte) 96, (byte) 133, (byte) 175, (byte) 144, (byte) 147,
			(byte) 229, (byte) 254, 86, (byte) 179, 74, 92, 34, 12, (byte) 205,
			(byte) 207, 110, (byte) 252, 51, 111, (byte) 197 };
	public static BigInteger TotalToken=total_amount.subtract(pre_ico_cap);
	public static Object Main(byte[] originator, String operation,
			byte[] args0, byte[] args1, byte[] args2) {
		String name = "Qlink Token";
		String symbol = "QLC";
		if (!Runtime.checkWitness(token_sales))
			if (Runtime.trigger() == TriggerType.Verification) {
				return Runtime.checkWitness(originator);
			} else if (Runtime.trigger() == TriggerType.Application) {
				if (operation == "init")
					return Init();
				if (operation == "name")
					return name;
				if (operation == "symbol")
					return symbol;
				if (operation == "totalSupply") 
					return TotalSupply();
                if (operation == "totalToken")
                	return TotalToken;
				if (operation == "mintTokens")
					return MintTokens();
				if (operation == "transfer")					
					Transfer(originator, args0, args1);
				if (operation == "balanceOf") {
	                return BalanceOf(args0);
	            }
				if (operation == "icoNeo") 
					 return IcoNeo();
	            if (operation == "endIco") 
	            	return EndIco();

			}
		return false;

	}

	public static BigInteger BalanceOf(byte[] address)
    {
       
        return Helper.asBigInteger(Storage.get(Storage.currentContext(), address));
    }
	

	private static boolean MintTokens() {
		
		byte[] sender=GetSender(); 
		if(sender.length!=0)
		{
			return false;
		}
        int now = Blockchain.getHeader(Blockchain.height()).timestamp();
        int time = now - ico_start_time.intValue();
        long contribute_value = GetContributeValue(time, sender);
        long swap_rate = CurrentSwapRate(time);
        if (swap_rate == 0)
        {
           return false;
        }        
        long token = CurrentSwapToken(sender, contribute_value, swap_rate);
        
        if(token == 0)
        {
            return false;
        }
           
        BigInteger balance = Helper.asBigInteger((Storage.get(Storage.currentContext(), sender)));
        BigInteger totalSupply = Helper.asBigInteger(Storage.get(Storage.currentContext(), "totalSupply"));    
        Storage.put(Storage.currentContext(), sender, BigInteger.valueOf(token).add(balance));
        Storage.put(Storage.currentContext(), "totalSupply", BigInteger.valueOf(token).add(totalSupply));
        Transferred(null, sender,BigInteger.valueOf(token));
		return true;
	}


	private static boolean Transfer(byte[] originator, byte[] to, byte[] amount) {
		BigInteger originatorValue = new BigInteger(Storage.get(
				Storage.currentContext(), originator));
		BigInteger targetValue = new BigInteger(Storage.get(
				Storage.currentContext(), to));

		BigInteger nOriginatorValue = originatorValue.subtract(new BigInteger(
				amount));
		BigInteger nTargetValue = targetValue.add(new BigInteger(amount));

		if (originatorValue.compareTo(BigInteger.ZERO) != -1
				&& new BigInteger(amount).compareTo(BigInteger.ZERO) != -1) {
			Storage.put(Storage.currentContext(), originator,
					nOriginatorValue.toByteArray());
			Storage.put(Storage.currentContext(), to,
					nTargetValue.toByteArray());
			return true;
		}
		return false;

	}

	public static BigInteger IcoToken() {
		BigInteger total_supply = new BigInteger(Storage.get(
				Storage.currentContext(), "totalSupply"));
		return total_supply.subtract(pre_ico_cap);
	}

	public static BigInteger IcoNeo() {
		BigInteger total_supply = new BigInteger(Storage.get(
				Storage.currentContext(), "totalSupply"));
		return (total_supply.subtract(pre_ico_cap)).divide(basic_rate);
	}

	public static BigInteger TotalSupply() {
		return new BigInteger(Storage.get(Storage.currentContext(),
				"totalSupply"));
	}

	public static Boolean Init() {
		byte[] total_supply = Storage.get(Storage.currentContext(),
				"totalsupply");
		if (total_supply.length != 0)
			return false;
		Storage.put(Storage.currentContext(), Other, pre_ico_cap);
		Storage.put(Storage.currentContext(), "totalSupply", pre_ico_cap);
		Transferred(null, token_sales, pre_ico_cap);
		return true;
	}

	public static Boolean EndIco() {

		BigInteger total_supply = new BigInteger(Storage.get(
				Storage.currentContext(), "totalSupply"));
		BigInteger remain_token = total_amount.subtract(total_supply);
		if (!Runtime.checkWitness(token_sales))
			return false;
		if (remain_token.intValue() <= 0) {
			return false;
		}
		Storage.put(Storage.currentContext(), "totalSupply",
				total_amount.toByteArray());
		Storage.put(Storage.currentContext(), token_sales,
				remain_token.toByteArray());
		Transferred(null, token_sales, remain_token);

		return true;

	}

	private static long CurrentSwapRate(int time)
    {
        if (time < 0)
        {
            return 0;
        }
        else if (time <= ico_duration.intValue())
        {
            return basic_rate.longValue();
        }
        else
        {
            return 0;
        }
    }
    private static long CurrentSwapToken(byte[] sender, long value, long swap_rate)
    {
        long token = value / neo_decimals * swap_rate;
        BigInteger total_supply = new BigInteger(Storage.get(Storage.currentContext(),"totalSupply"));
        BigInteger balance_token = total_amount.subtract(total_supply);
        if (balance_token.longValue() <= 0)
        {
            Refund(sender, value);
            return 0;
        }
        else if (balance_token.compareTo(BigInteger.valueOf(token))==-1)
        {
        	Refund(sender, (token - balance_token.longValue()) / swap_rate * neo_decimals);
            token =balance_token.longValue();
        }
        return token;
       
    }

	private static void Refund(byte[] sender, long value) {
		// TODO Auto-generated method stub

	}

	private static long getOutputValue() {
		Transaction tx = (Transaction) ExecutionEngine.scriptContainer();
		TransactionOutput[] outputs = tx.outputs();
		long value = 0;
		for (TransactionOutput output : outputs) {
			{
				if (output.scriptHash() == getReceiver()
						&& output.assetId() == neo_asset_id) {
					value += output.value();
				}

			}
		}
		return value;
	}

	private static byte[] getReceiver() {
		// TODO Auto-generated method stub
		return ExecutionEngine.entryScriptHash();
	}

		

	
	private static byte[] GetSender() { Transaction tx = (Transaction)
	 ExecutionEngine.scriptContainer(); TransactionOutput[] reference =
	 tx.references(); for (TransactionOutput output : reference) {
	 if(output.assetId()==neo_asset_id) return output.scriptHash();
	 }
	 return null; 
	}
	 
	 private static long GetContributeValue(int time, byte[] sender)
    {
         long value = getOutputValue();
         if (time > 0 && time <= limit_time.intValue())
         {
        	 	
        	 byte[] key=Helper.concat(Helper.asByteArray(contribute_prefix), sender);
        	         
        	 BigInteger total_neo=new BigInteger(Storage.get(Storage.currentContext(),key));
        	 long balance_neo = limit_neo -total_neo.longValue();
             if (balance_neo <= 0)
             {
                 Refund(sender, value);
                 return 0;
             }
             else if (balance_neo < value)
             {
            	 Storage.put(Storage.currentContext(),key,BigInteger.valueOf(balance_neo).add(total_neo));
                 Refund(sender, value - balance_neo);
                 return balance_neo;
             }
             Storage.put(Storage.currentContext(),key,BigInteger.valueOf(value).add(total_neo));
            
         }
         return value;
     }
 
	private static void Transferred( byte[] args,
			byte[] args1,BigInteger args2) {
		// TODO Auto-generated method stub

	}
}
