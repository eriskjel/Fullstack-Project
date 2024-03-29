<template>
    <div class="wrapper">
      <form @submit.prevent="sendForm()" class="form">
        <h1>Edit Listing</h1>
        <div class="category">
          <h3 v-if="icon">Current category: {{ icon.type }}</h3>
          <h3 v-else>Current category: {{ props.item.categoryId }}</h3>
        <div class ="categoryDropdown">
            <select v-model="selectedCategory">
            <option value="">All</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.type }}</option>
            </select>
        </div>
        <font-awesome-icon :icon="icon.iconUrl" style="display: inline; padding-top: 4px" />
        </div>


        <div v-if="icon" class="icon-container"></div>
        <BaseInput id="inpTitle" class="input-container" type="text" label="Title" v-model="form.title" />
        <BaseInput id="inpDescription" class="input-container" type="text" label="Description" v-model="form.description" />
        <BaseInput id="inpPrice" class="input-container" type="number" label="Price" v-model="form.price" />
        <BaseInput id="inpLongitude" class="input-container" type="number" label="Longitude" v-model="form.longitude" />
        <BaseInput id="inpLatitude" class="input-container" type="number" label="Latitude" v-model="form.latitude" />
        <MapComponent :latitude="form.latitude" :longitude="form.longitude" @set-location="(lat, long) => setLocation(lat,long)" />
        <div class="image-gallery">
          <div v-for="(image, index) in images" :key="index" class="image-wrapper">
            <img :src="image.url" :alt="image.name" />
            <button @click="removeImage(index)">Remove</button>
          </div>
        </div>
        <div class="input-container">
          <input id="inpImages" type="file" @change="onImagesChange" multiple />
        </div>
        <button id="button">Create Listing</button>
        <ErrorMessage v-if="errorMessage" :message="errorMessage" @clear-error="errorMessage=''"/>
      </form>
    </div>
  </template>

  <script setup lang="ts">
  import BaseInput from "@/components/Form/BaseInput.vue";
  import {defineProps, onMounted, ref} from "vue";
  import router from "@/router";
  import axiosInstance from "@/service/AxiosInstance";
  import {useItemStore} from "@/stores/Item";
  import ErrorMessage from "@/components/Errors/ErrorMessage.vue";
  import MapComponent from "@/components/Map/MapComponent.vue";
  import type {Item} from "@/types/ItemType";
  import type {Category} from "@/types/CategoryType";

  const itemStore = useItemStore();

  const props = defineProps({
    item:{
        type: Object as () => Item,
        required:true,
    }
  })

  const errorMessage = ref("");

  interface Form {
    title: string;
    description: string;
    price: string;
    longitude: number;
    latitude: number;
    images: File[];
    [key: string]: string | File[] | number;
  }

  const form = ref<Form>({
    title: "",
    description: "",
    price: "",
    longitude: 60,
    latitude: 10,
    images: [],
  });

  function setLocation(newLatitude : number, newLongitude:number){
    form.value.latitude = newLatitude;
    form.value.longitude = newLongitude;
  }

  let icon = ref({
    type: "",
    iconUrl: "",
  });

  let images = ref<{ url: string; name: string }[]>([]);
  const categories = ref<Category[]>([]);
  const selectedCategory = ref<number | null>(null);

  const validateForm = () => {
    errorMessage.value = "";

    if (!form.value.title || form.value.title.length < 3 || form.value.title.length > 50) {
      errorMessage.value = "Title must be at least 3 characters and cannot exceed 50";
      return false
    }

    if (!form.value.description || form.value.description.length < 20 ||form.value.description.length > 1000) {
      errorMessage.value = "Description must be at least 20 characters and cannot exceed 1000";
      return false
    }

    if (!form.value.price || isNaN(Number(form.value.price)) || Number(form.value.price) < 10 || Number(form.value.price) > 10000000) {
      errorMessage.value = "Price must be a valid number between 10 and 10.000.000";
      return false;
    }

    if (!form.value.longitude || isNaN(Number(form.value.longitude))
        || Number(form.value.longitude) < -180 || Number(form.value.longitude) > 180) {
      errorMessage.value = "Longitude must be a valid number between -180 and 180";
      return false;
    }

    if (!form.value.latitude || isNaN(Number(form.value.latitude))
        || Number(form.value.latitude) < -90 || Number(form.value.latitude) > 90) {
      errorMessage.value = "Latitude must be a valid number between -90 and 90";
      return false;
    }

    return true;
  };

  const onImagesChange = (event: Event) => {
    const target = event.target as HTMLInputElement;
    if (!target.files) {
      return;
    }
    if (form.value.images.length + target.files.length > 5) {
      errorMessage.value = "You can only upload up to 5 images.";
      return;
    }
    const files = Array.from(target.files);
    form.value.images = [...form.value.images, ...files];
    images.value = [...images.value, ...files.map((file) => ({ url: URL.createObjectURL(file), name: file.name }))];
  };

  const removeImage = (index: number) => {
    images.value.splice(index, 1);
    form.value.images.splice(index, 1);
  };


  const fetchIcon = async (chosenCategory: any) => {
    try {
      const response = await axiosInstance.get(`/api/${chosenCategory}/icon`);
      icon.value = response.data;
    } catch (error) {
      console.error(error);
    }
  };

  function loadForm(){
    form.value.title = props.item.title;
    form.value.description = props.item.description;
    form.value.price = props.item.price.toString();
    form.value.latitude = props.item.latitude;
    form.value.longitude = props.item.longitude;
    selectedCategory.value = props.item.categoryId;
    form.value.images = convertToFiles(props.item.images);
    images.value = props.item.images.map((image) => ({ url: image.data.toString(), name: "image" }));
}

  function convertToFiles(images: any[]) {
    return images.map((image) => {
      const byteString = atob(image.data.split(',')[1]);
      const arrayBuffer = new ArrayBuffer(byteString.length);
      const uint8Array = new Uint8Array(arrayBuffer);
      for (let i = 0; i < byteString.length; i++) {
        uint8Array[i] = byteString.charCodeAt(i);
      }
      const blob = new Blob([arrayBuffer], { type: image.contentType });
      const file = new File([blob], `image.${image.contentType.split("/")[1]}`, {
        type: image.contentType,
      });
      return file;
    });
  }

  onMounted(async () => {
    await fetchIcon(itemStore.getNewListingCategory);
    try {
    const response = await axiosInstance.get('/api/categories');
    categories.value = response.data;

  } catch (error) {
    console.error(error);
  }
  loadForm();


  });

  const sendForm = async () => {
  if (!validateForm()) {
    return;
  }
  try {
    const formData = new FormData();
    formData.append("id", props.item.id.toString());
    formData.append("title", form.value.title);
    formData.append("description", form.value.description);
    formData.append("price", form.value.price);
    formData.append("category_id", selectedCategory.value!.toString());
    formData.append("longitude", form.value.longitude.toString());
    formData.append("latitude", form.value.latitude.toString());
    if(form.value.images.length == 0){
        formData.append("images", new File([],"image_"));
    }
    else{
        for (let i = 0; i < form.value.images.length; i++) {
        const file = new File([form.value.images[i]], "image_" + i, { type: form.value.images[i].type });
        formData.append("images", file);
    }
    }
    const response = await axiosInstance.post("/api/items/edit-listing", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    if (response.status === 200) {
      form.value.title = "";
      form.value.description = "";
      form.value.price = "";
      form.value.longitude = 60;
      form.value.latitude = 10;
      form.value.images = [];
      images.value = [];
      await router.push({
        name:"item-details",
        params:{
            id:props.item.id
        }
      });
    }
  } catch (error: any) {
    errorMessage.value = "Error " + error.request.status + ": " + error.response.data;
  }
};
  </script>

  <style scoped>


  #changeCategoryBtn {
    cursor: pointer;
  }
  .category {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .image-wrapper {
    padding-top: 3rem;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .image-wrapper img {
    max-width: 100%;
    height: auto;
    max-height: 200px;
    margin-bottom: 10px;
  }

  .image-wrapper button {
    background-color: red;
    color: white;
    border: none;
    padding: 5px 10px;
    border-radius: 5px;
    font-size: 16px;
    cursor: pointer;
  }

  .category h3 {
    margin-right: 10px;
    cursor: default;
  }
  h1{
    color: white;
    text-align: center;
    font-size: 40px;
    font-weight: 300;
  }

  h3{
    color: white;
    text-align: center;
    font-size: 20px;
    font-weight: 300;
  }

  h4{
    color: white;
    text-align: center;
    font-size: 15px;
    font-weight: 300;
  }

  textarea{
    resize: none;
    margin-top: 20px;
  }
  .form {
    margin: 50px auto 0 auto;
    width: 40%;
    height: fit-content;
    background-image: linear-gradient(to bottom right, rgb(92, 88, 88), rgb(37, 33, 33));
    padding: 40px;
    border-radius: 15px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  }


  .input-container {
    margin-top: 40px;
    height: 50px;
    position: relative;
    width: 100%;
    margin-bottom: 20px;
  }

  .input:focus ~ .cut,
  .input:not(:placeholder-shown) ~ .cut {
    transform: translateY(8px);
    background-color: rgba(92, 88, 88,1);
  }

  ::placeholder {
    color: white;
  }

  .input:focus ~ .placeholder,
  .input:not(:placeholder-shown) ~ .placeholder {
    transform: translateY(-30px) translateX(10px) scale(0.75);
  }

  .input:not(:placeholder-shown) ~ .placeholder {
    color: #808097;
  }

  .input:focus ~ .placeholder {
    color: white;
  }

  #button {
    background: linear-gradient(45deg, #FC466B, #3F5EFB);
    border: 0;
    border-radius: 12px;
    color: white;
    cursor: pointer;
    font-size: 18px;
    height: 50px;
    margin-top: 40px;
    outline: 0;
    width: 100%;
    transition: transform 200ms;
  }

  #button:hover {
    transform: scale(1.05);
  }

  #button:disabled{
    cursor: initial;
    transform: scale(1);
  }
  /* Popup container - can be anything you want */
  .popup {
    position: relative;
    display: inline-block;
    cursor: pointer;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
    user-select: none;
    left: 50%;
  }

  /* The actual popup */
  .popup .popuptext {
    visibility: hidden;
    width: 160px;
    background-color: #555;
    color: #fff;
    text-align: center;
    border-radius: 6px;
    padding: 8px 0;
    position: absolute;
    z-index: 1;
    bottom: 125%;
    left: 50%;
    margin-left: -80px;
    margin-bottom: -35px;
    cursor: default;
  }

  /* Popup arrow */
  .popup .popuptext::after {
    content: "";
    position: absolute;
    top: 100%;
    left: 50%;
    margin-left: -5px;
    border-width: 5px;
    border-style: solid;
    border-color: #555 transparent transparent transparent;
  }

  /* Toggle this class - hide and show the popup */
  .popup .show {
    visibility: visible;
    -webkit-animation: fadeIn 1s;
    animation: fadeIn 1s;
  }

  /* Add animation (fade in the popup) */
  @-webkit-keyframes fadeIn {
    from {opacity: 0;}
    to {opacity: 1;}
  }

  @keyframes fadeIn {
    from {opacity: 0;}
    to {opacity:1 ;}
  }

  .text{
    color: white;
    font-size: 15px;
  }

  .link{
    color: white;
    font-size: 15px;
  }

  .link:hover{
    color: white;
    font-size: 15px;
    text-decoration: underline;
  }
  .categoryDropdown {
  margin-top: 10px;
}

.categoryDropdown label {
  display: inline-block;
  margin-bottom: 5px;
}

.categoryDropdown select {
  display: block;
  width: 100%;
  padding: 5px;
  font-size: 14px;
  border-radius: 4px;
  border: 1px solid #646464;
  background-color: #ffffff;
  color: #333333;
  margin-bottom: 5%;
}

  @media screen and (max-width: 768px) {
    .form {
      width: 90%;
      padding: 30px;
    }

    h1 {
      font-size: 30px;
    }

    h3 {
      font-size: 18px;
    }

    h4 {
      font-size: 12px;
    }

    .input {
      font-size: 16px;
    }

    #button {
      font-size: 16px;
      height: 40px;
    }

    .input-container {
      margin-top: 30px;
      height: 40px;
    }

    .cut {
      width: 60px;
    }

    .placeholder {
      font-size: 12px;
    }
  }

  </style>