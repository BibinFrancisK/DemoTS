import { Lambda } from 'aws-sdk';
import { handler } from './handler'; // Assuming the original code is in a file named "handler.ts"

// Mock the AWS Lambda invoke method
jest.mock('aws-sdk', () => {
  return {
    Lambda: jest.fn(() => ({
      invoke: jest.fn().mockReturnThis(),
      promise: jest.fn()
    }))
  };
});

describe('Lambda Handler Test', () => {
  const mockInvokeResult = {
    Payload: JSON.stringify({ result: 'data from SecondLambda' })
  };

  it('should return data from SecondLambda when memberId is provided', async () => {
    // Arrange
    const event = { encryptedMemberId: 'encryptedId123' };
    const lambdaMock = new Lambda() as jest.Mocked<Lambda>; // Typecast to mocked version

    // Mock the Lambda.invoke().promise() to return the mockInvokeResult
    lambdaMock.invoke().promise.mockImplementationOnce(() => Promise.resolve(mockInvokeResult));

    // Act
    const result = await handler(event, null);

    // Assert
    expect(result.statusCode).toBe(200);
    expect(result.body).toBe(JSON.stringify({ result: 'data from SecondLambda' }));
    expect(lambdaMock.invoke).toHaveBeenCalledWith({
      FunctionName: 'SecondLambda',
      Payload: Buffer.from(JSON.stringify({ encryptedMemberId: 'encryptedId123' }))
    });
  });

  // ... Other test cases
});
