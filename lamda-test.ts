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
    lambdaMock.invoke().promise.mockResolvedValueOnce(mockInvokeResult);

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

  it('should return 400 status when memberId is missing', async () => {
    // Arrange
    const event = {};
    const lambdaMock = new Lambda() as jest.Mocked<Lambda>; // Typecast to mocked version

    // Act
    const result = await handler(event, null);

    // Assert
    expect(result.statusCode).toBe(400);
    expect(result.body).toBe('Missing memberId in payload');
    expect(lambdaMock.invoke).not.toHaveBeenCalled(); // Ensure Lambda.invoke() is not called
  });

  it('should return 500 status when Lambda.invoke() does not return Payload', async () => {
    // Arrange
    const event = { encryptedMemberId: 'encryptedId123' };
    const lambdaMock = new Lambda() as jest.Mocked<Lambda>; // Typecast to mocked version

    // Mock the Lambda.invoke().promise() to return undefined
    lambdaMock.invoke().promise.mockResolvedValueOnce({});

    // Act
    const result = await handler(event, null);

    // Assert
    expect(result.statusCode).toBe(500);
    expect(result.body).toBe('Error invoking SecondLambda');
    expect(lambdaMock.invoke).toHaveBeenCalled();
  });
});
