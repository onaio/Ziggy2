package com.sample.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

public class Main {
	
	public static void main(String[] ards){
		
		StringBuilder sb = new StringBuilder();
		
		File file = new File("input");
		FileInputStream fis = null;
		BufferedReader br = null;

		try {
			fis = new FileInputStream(file);
			
			System.out.println("Total file size to read (in bytes) : " + fis.available());
			
			br = new BufferedReader(new InputStreamReader(fis));
			
			String token = null;
			while((token = br.readLine()) != null){
				sb.append(token);
			}
            
            JSONObject json = XML.toJSONObject(sb.toString());
            System.out.println(json);
            Iterator<?> keys = json.keys();
			while( keys.hasNext() ) {
			    String key = (String)keys.next();
			    if (json.get(key) instanceof JSONObject ) {
			    	saveJsonObjectFields(json.getJSONObject(key), key, null, null);
			    }
			}
            
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fis != null)
					fis.close();
				if(br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	static int dummyIndex = 1;
	
	private static Long saveJsonObjectFields(JSONObject jsonObject, String tableName, String foreignIdFieldName, Long foreignId){
		try {
			StringBuilder createTableSql = new StringBuilder("create table if not exists " + tableName + "(_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT");
			StringBuilder insertSqlStmt = new StringBuilder("insert into " + tableName + " "); 
			StringBuilder columnsString = new StringBuilder("(");
			StringBuilder valuesString = new StringBuilder("values(");
			
			if (foreignIdFieldName != null && foreignId != null) {
				createTableSql.append(", " + foreignIdFieldName + " INTEGER NOT NULL");
				columnsString.append(foreignIdFieldName + ", ");
				valuesString.append(foreignId + ", ");
			}
			
			List<String> fields = new ArrayList<String>();
			Iterator<?> keys = jsonObject.keys();
			while( keys.hasNext() ) {
			    String key = (String)keys.next();
			    fields.add(key);
			}
			
			/*
			 * generate create table & insert sql statement
			 */
			for(String field : fields) {
				createTableSql.append(", " + field + " TEXT"); // let all the fields be varchar
				columnsString.append(field + ", ");
				
				if (jsonObject.get(field) instanceof JSONObject ) {
					valuesString.append("\\\"FETCH_OBJECT\\\", ");
			    }else if(jsonObject.get(field) instanceof JSONArray ) {
			    	valuesString.append("\\\"FETCH_OBJECT\\\", ");
			    }else {
			    	valuesString.append("\\\"" + jsonObject.get(field).toString() + "\\\", ");
			    }
			}
			createTableSql.append(");");
			executeCreateTableIfNotExistStatement(createTableSql.toString());
			
			columnsString.deleteCharAt(columnsString.length() - 2).append(")"); //remove trailing ,
			valuesString.deleteCharAt(valuesString.length() - 2).append(");"); //remove trailing ,
			insertSqlStmt.append(columnsString.toString()).append(" ").append(valuesString.toString());
			
			/*
			 * generate the id for this record and fetch/create child records
			 */
			Long id = executeInsertStatement(insertSqlStmt.toString());
			for(String field : fields) {
				if (jsonObject.get(field) instanceof JSONObject ) {
					//concat the table name and field to get the name of the child table
					saveJsonObjectFields(jsonObject.getJSONObject(field), tableName+"__"+field, tableName, id);
			    }else if(jsonObject.get(field) instanceof JSONArray ) {
			    	valuesString.append("\\\"FETCH_OBJECT\\\", ");
			    	JSONArray array = jsonObject.getJSONArray(field);
			    	for (int i = 0; i < array.length(); i++) {
			    		if (array.get(i) instanceof JSONObject) {
			    			//concat the table name and field to get the name of the child table
			    			saveJsonObjectFields(array.getJSONObject(i), tableName+"__"+field, tableName, id);
						}
					}
			    }
			}
			
			return id;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	/*
	 * Insert the a new record to the database and returns its id, 
	 * on the Android client side the logic might change a little but the general idea is basically to insert and get the id
	 */
	private static Long executeInsertStatement(String insertSqlStmt) {
		// TODO Auto-generated method stub
		System.out.println(insertSqlStmt);
		return Long.valueOf(dummyIndex++);
	}

	/*
	 * Create a table if it does not exist
	 */
	private static void executeCreateTableIfNotExistStatement(String createTableSql) {
		// TODO Auto-generated method stub
		System.out.println(createTableSql);
	}
	
}
