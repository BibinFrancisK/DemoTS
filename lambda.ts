import { Lambda } from 'aws-sdk';
import { Handler } from 'aws-lambda';

export const handler: Handler = async (event, context) => {
  try {
    const encryptedMemberId = event?.encryptedMemberId.toString();
    if (!encryptedMemberId) {
      return {
        statusCode: 400,
        body: 'Missing memberId in payload'
      };
    }

    const lambda = new Lambda();

    const params = {
      FunctionName: 'SecondLambda', 
      Payload: Buffer.from(JSON.stringify({ encryptedMemberId: encryptedMemberId}))
    };

    const result = await lambda.invoke(params).promise();

    if (!result.Payload) {
      return {
        statusCode: 500,
        body: 'Error invoking SecondLambda'
      };
    }

    const responseFromDynamoDB = JSON.parse(result.Payload.toString());

    return {
      statusCode: 200,
      body: responseFromDynamoDB
    };
  } catch (error) {
    console.error('Error calling SecondLambda:', error);
    throw error;
  }
};
