import { LambdaClient, InvokeCommand } from '@aws-sdk/client-lambda';
import { handler } from './handler'; // Assuming the original code is in a file named "handler.ts"

describe('Lambda Handler Test', () => {
  it('should return member details when encryptedMemberId is provided', async () => {
    // Arrange
    const encryptedMemberId = 'encryptedId123';
    const event = { encryptedMemberId: encryptedMemberId };

    const mockPayload = JSON.stringify({ /* mock member details here */ });
    const mockResponse = { Payload: new TextEncoder().encode(mockPayload) };

    const mockSend = jest.fn().mockResolvedValueOnce(mockResponse);
    const mockLambdaClient = {
      send: mockSend,
    } as unknown as LambdaClient;

    // Act
    const result = await handler(event, null, mockLambdaClient);

    // Assert
    expect(result.statusCode).toBe(200);
    expect(result.body).toBe(mockPayload);

    expect(mockSend).toHaveBeenCalledWith(expect.any(InvokeCommand));
    const invokeCommand: InvokeCommand = mockSend.mock.calls[0][0];
    expect(invokeCommand.input.FunctionName).toBe('arn:aws:lambda:us-east-1:148795117399:function:GetMembersFunction');
    expect(invokeCommand.input.Payload).toBeInstanceOf(Uint8Array);
  });

  it('should return 400 status when encryptedMemberId is missing', async () => {
    // Arrange
    const event = {};

    // Act
    const result = await handler(event);

    // Assert
    expect(result.statusCode).toBe(400);
    expect(result.body).toBe('encryptedMemberId is missing');
  });

  it('should return 500 status when Lambda response has no Payload', async () => {
    // Arrange
    const encryptedMemberId = 'encryptedId123';
    const event = { encryptedMemberId: encryptedMemberId };

    const mockSend = jest.fn().mockResolvedValueOnce({});
    const mockLambdaClient = {
      send: mockSend,
    } as unknown as LambdaClient;

    // Act
    const result = await handler(event, null, mockLambdaClient);

    // Assert
    expect(result.statusCode).toBe(500);
    expect(result.body).toBe('lambda did not return any result');
  });

  it('should handle errors and re-throw them', async () => {
    // Arrange
    const encryptedMemberId = 'encryptedId123';
    const event = { encryptedMemberId: encryptedMemberId };

    const mockError = new Error('Mock LambdaClient Error');
    const mockSend = jest.fn().mockRejectedValueOnce(mockError);
    const mockLambdaClient = {
      send: mockSend,
    } as unknown as LambdaClient;

    // Act and Assert
    await expect(handler(event, null, mockLambdaClient)).rejects.toThrow(mockError);
  });
});
