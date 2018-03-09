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

public class IcoTemplate extends SmartContract {
	public static String name = "Wandxsample";
	public static String symbol = "wannds";
	public static byte[] owner = "ATrzHaicmhRj15C3Vv6e6gLfLqhSD2PtTr".getBytes();
	public static BigInteger Decimals =BigInteger.valueOf(8);
	private static long factor = 100000000; // decided by Decimals()
	private static long neo_decimals = 100000000;

	// ICO Settings
	private static byte[] neo_asset_id = { (byte) 155, 124, (byte) 255,
			(byte) 218, (byte) 166, 116, (byte) 190, (byte) 174, 15,
			(byte) 147, 14, (byte) 190, 96, (byte) 133, (byte) 175, (byte) 144,
			(byte) 147, (byte) 229, (byte) 254, 86, (byte) 179, 74, 92, 34, 12,
			(byte) 205, (byte) 207, 110, (byte) 252, 51, 111, (byte) 197 };
	private static long total_amount = 100000000 * factor; // total token amount
	private static long pre_ico_cap = 30000000 * factor; // pre ico token amount
	private static long basic_rate = 1000 * factor;
	private static int ico_start_time = 1506787200;
	private static int ico_end_time = 1538323200;

	public static Object Main(String operation, Object[] args) {
		if (Runtime.trigger() == TriggerType.Verification) {
			if (owner.length == 20) {
				return Runtime.checkWitness(owner);
			} else if (owner.length == 33) {

				byte[] signature = Helper.asByteArray(operation);
				return verifySignature(signature, owner);
			}
		}
		else if (Runtime.trigger()==TriggerType.Application) {
			if(operation=="deploy")return Deploy();
			if (operation == "mintTokens") return MintTokens();
            if (operation == "totalSupply") return TotalSupply();
            if (operation == "name") return name;
            if (operation == "symbol") return symbol;
            if (operation == "decimals") return Decimals;
			if (operation == "transfer")
            {
                if (args.length != 3) return false;
                byte[] from = (byte[])args[0];
                byte[] to = (byte[])args[1];
                BigInteger value = BigInteger.valueOf((long) args[2]);
                return Transfer(from, to, value);
            }
			if (operation == "balanceOf")
            {
                if (args.length != 1) return 0;
                byte[] account = (byte[])args[0];
                return BalanceOf(account);
            }
            
        }
			
		 byte[] sender = GetSender();
         long contribute_value = GetContributeValue();
         if (contribute_value > 0 && sender.length != 0)
         {
             Refund(sender, contribute_value);
         }
		return false;

	}

	private static Boolean Deploy() {
		// TODO Auto-generated method stub
		 byte[] total_supply = Storage.get(Storage.currentContext(), "totalSupply");
         if (total_supply.length != 0) return false;
         Storage.put(Storage.currentContext(), owner, BigInteger.valueOf(pre_ico_cap));
         Storage.put(Storage.currentContext(), "totalSupply", BigInteger.valueOf(pre_ico_cap));
         Transferred(null, owner, BigInteger.valueOf(pre_ico_cap));
         return true;
	}

	
	private static Boolean Transfer(byte[] from, byte[] to, BigInteger value) {
		// TODO Auto-generated method stub
		if(value.longValue()<=0)return false;
		if(!Runtime.checkWitness(from))return false;
		if (from == to) return true;
		BigInteger from_value=Helper.asBigInteger(Storage.get(Storage.currentContext(),to));
		if(from_value.intValue()<value.intValue())return false;
		if (from_value == value)
            Storage.delete(Storage.currentContext(), from);
        else
        	Storage.put(Storage.currentContext(), from, from_value.subtract(value));
			BigInteger to_value=Helper.asBigInteger(Storage.get(Storage.currentContext(),to));
	        Storage.put(Storage.currentContext(), to, to_value.add(value));
	        Transferred(from, to, value);
	        return true;
	}
	public static Boolean MintTokens()
    {
        byte[] sender = GetSender();
                if (sender.length == 0)
        {
            return false;
        }
        long contribute_value = GetContributeValue();
        long swap_rate = CurrentSwapRate();
     
        if (swap_rate == 0)
        {
            Refund(sender, contribute_value);
            return false;
        }
        // you can get current swap token amount
        long token = CurrentSwapToken(sender, contribute_value, swap_rate);
        if (token == 0)
        {
            return false;
        }
        BigInteger balance=Helper.asBigInteger(Storage.get(Storage.currentContext(),sender));
        Storage.put(Storage.currentContext(), sender, BigInteger.valueOf(token).add(balance));
        BigInteger totalSupply=Helper.asBigInteger(Storage.get(Storage.currentContext(),"totalSupply"));
        Storage.put(Storage.currentContext(), "totalSupply", BigInteger.valueOf(token).add(totalSupply));
        Transferred(null, sender, BigInteger.valueOf(token));
        return true;
    }

	
		private static byte[] GetSender()
	    {
			Transaction tx=(Transaction) ExecutionEngine.scriptContainer();
			TransactionOutput[] reference=tx.references();
			for (TransactionOutput output : reference) {
				if(output.assetId()==neo_asset_id)return output.scriptHash();
			}
			return null;    
	             
	    }
		 public static BigInteger BalanceOf(byte[] address)
	        {
	         //return Helper.asBigInteger(Storage.get(Storage.currentContext(),address));  
	         return new BigInteger(Storage.get(Storage.currentContext(),address));
			 
	        }
	private static byte[] GetReceiver()
    {
        return ExecutionEngine.executingScriptHash();
    }
	private static long GetContributeValue()
    {
        Transaction tx = (Transaction)ExecutionEngine.scriptContainer();
        TransactionOutput[] outputs = tx.outputs();
        long value = 0;
        for (TransactionOutput output: outputs) {
			if(output.scriptHash()==GetReceiver()&&output.assetId()==neo_asset_id)
			{
				value+=output.value();
			}
		}
      
        return value;
    }
	private static long CurrentSwapToken(byte[] sender, long value, long swap_rate)
    {
        long token = value / neo_decimals * swap_rate;
        BigInteger total_supply = new BigInteger(Storage.get(Storage.currentContext(),"totalSupply"));
        BigInteger balance_token = BigInteger.valueOf(total_amount).subtract(total_supply);
        if (balance_token.intValue() <= 0)
        {
            Refund(sender, value);
            return 0;
        }
        else if (balance_token.longValue()< token)
        {
        	Refund(sender, (token - balance_token.longValue()) / swap_rate * neo_decimals);
            token = balance_token.longValue();
         }
     
        return token;
    }
	 private static long CurrentSwapRate()
     {
          int ico_duration = ico_end_time - ico_start_time;
          int now = Blockchain.getHeader(Blockchain.height()).timestamp();
	         int time = (int)now - ico_start_time;
	         if (time < 0)
	         {
	             return 0;
	         }
	         else if (time < ico_duration)
	         {
	             return basic_rate;
	         }
	         else
	         {
	             return 0;
	         }
     }
	 public static BigInteger TotalSupply()
     {
		 return new BigInteger(Storage.get(Storage.currentContext(),"totalsupply"));
         
     }



	private static void Refund(byte[] sender, long value) {
		// TODO Auto-generated method stub
		
	}

	private static void Transferred(byte[] from, byte[] to, BigInteger value) {
		// TODO Auto-generated method stub
		
	}

}
