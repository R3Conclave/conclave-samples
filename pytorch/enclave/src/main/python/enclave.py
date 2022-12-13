# This PyTorch image classification example is based off
# https://www.learnopencv.com/pytorch-for-beginners-image-classification-using-pre-trained-models/

from torchvision import transforms
from PIL import Image
import torch
from io import BytesIO

transform = None
model = None
classes = None

def on_enclave_startup():
    # Prepare a transform to get the input image into a format (e.g., x,y dimensions) the classifier
    # expects.
    global transform
    transform = transforms.Compose([
        transforms.Resize(256),
        transforms.CenterCrop(224),
        transforms.ToTensor(),
        transforms.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225]
        )])
    print("Enclave ready...")

def receive_enclave_mail(mail):
    f = BytesIO(mail.body)
    # Read the command byte from the Mail body
    command, = f.read(1)
    if command == 1:  # Provision model and classes
        # Read the model from the Mail body
        model_size = int.from_bytes(f.read(4), byteorder='big')
        global model
        model = torch.load(BytesIO(f.read(model_size)))
        # Read the classes list from the Mail body
        global classes
        classes = [line.strip().decode("utf-8") for line in f.readlines()]
        return "OK".encode("utf-8")
    elif command == 2:  # Classify provided image
        # Read the image from the Mail body
        image = Image.open(BytesIO(f.read()))
        # Apply the transform to the image.
        image_t = transform(image)
        # Magic (not sure what this does).
        batch_t = torch.unsqueeze(image_t, 0)
        # Prepare the model and run the classifier.
        model.eval()
        out = model(batch_t)
        # Sort the predictions.
        _, indices = torch.sort(out, descending=True)
        # Convert into percentages.
        percentage = torch.nn.functional.softmax(out, dim=1)[0] * 100
        top_class = classes[indices[0][0]]
        top_percent = percentage[indices[0][0]].item()
        return f"{top_class} ({top_percent:.1f}%)".encode("utf-8")
    else:
        return f"Unknown command {command}".encode("utf-8")
